package com.insurmatch.service;

import com.insurmatch.dto.deal.CreateDealRequest;
import com.insurmatch.dto.deal.DealResponse;
import com.insurmatch.dto.deal.UpdateStageRequest;
import com.insurmatch.entity.*;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DealService {

    private final DealRepository dealRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final ActivityRepository activityRepository;
    private final NoteRepository noteRepository;
    private final TaskRepository taskRepository;
    private final ContactService contactService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy, HH:mm");
    private static final Random RANDOM = new Random();

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    public List<DealResponse> getAllDeals(String search, String stage, String pipeline, String carrier, String owner) {
        Long ownerId = null;
        if (owner != null && !owner.isBlank() && !owner.equalsIgnoreCase("all")) {
            try {
                ownerId = Long.parseLong(owner);
            } catch (NumberFormatException e) {
                List<User> found = userRepository.searchByNameOrEmail(owner);
                if (!found.isEmpty()) {
                    ownerId = found.get(0).getId();
                }
            }
        }

        // RBAC: If authenticated user is an AGENT, restrict strictly to their own deals
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            ownerId = currentUser.getId();
            log.info("RBAC: Scoping deals strictly for agent: {} (id: {})", currentUser.getEmail(), currentUser.getId());
        }

        List<Deal> deals = dealRepository.filterDeals(
                search != null && !search.isBlank() ? search : null,
                pipeline != null && !pipeline.isBlank() ? pipeline : null,
                stage != null && !stage.isBlank() ? stage : null,
                carrier != null && !carrier.isBlank() ? carrier : null,
                ownerId
        );

        return deals.stream()
                .map(d -> DealResponse.fromEntity(d, null, null, null, null))
                .collect(Collectors.toList());
    }

    public Deal getDealById(Long id) {
        return dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found with id: " + id));
    }

    public Deal getDealByIdOrCode(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResourceNotFoundException("Deal identifier cannot be empty");
        }
        java.util.Optional<Deal> byCode = dealRepository.findByCode(identifier);
        if (byCode.isPresent()) {
            Deal d = byCode.get();
            validateAgentAccess(d);
            return d;
        }
        try {
            Long id = Long.parseLong(identifier);
            return getDealById(id);
        } catch (NumberFormatException ignored) {}

        String digits = identifier.replaceAll("^[^0-9]+", "");
        if (digits.startsWith("2600") && digits.length() > 4) {
            digits = digits.substring(4);
        }
        try {
            long num = Long.parseLong(digits);
            if (num > 5000) {
                try {
                    return getDealById(num - 5000);
                } catch (Exception ignored) {}
            }
            return getDealById(num);
        } catch (NumberFormatException | ResourceNotFoundException ex) {
            throw new ResourceNotFoundException("Deal not found with identifier: " + identifier);
        }
    }

    private void validateAgentAccess(Deal deal) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            if (deal.getDealOwner() == null || !deal.getDealOwner().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Bạn chỉ có quyền xem hợp đồng do mình phụ trách!");
            }
        }
    }

    public DealResponse getDealResponseById(Long id) {
        Deal deal = getDealById(id);
        validateAgentAccess(deal);

        List<Activity> activities = activityRepository.findByDealIdOrderByCreatedAtDesc(id);
        List<Note> notes = noteRepository.findByDealIdOrderByCreatedAtDesc(id);
        List<Task> tasks = taskRepository.findByDealId(id);
        List<Ticket> tickets = ticketRepository.findByDealId(id);
        return DealResponse.fromEntity(deal, activities, notes, tasks, tickets);
    }

    public DealResponse getDealResponseById(String identifier) {
        Deal deal = getDealByIdOrCode(identifier);
        return getDealResponseById(deal.getId());
    }

    @Transactional
    public DealResponse createDeal(CreateDealRequest req) {
        // 1. Resolve Deal Owner
        User currentUser = getCurrentAuthenticatedUser();
        User owner = null;
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            // RBAC: Agent deals are always strictly owned by the creating Agent
            owner = currentUser;
        } else {
            if (req.getDealOwnerId() != null) {
                owner = userRepository.findById(req.getDealOwnerId()).orElse(null);
            }
            if (owner == null && req.getDealOwner() != null && !req.getDealOwner().isBlank() && !req.getDealOwner().equals("--")) {
                List<User> users = userRepository.searchByNameOrEmail(req.getDealOwner());
                if (!users.isEmpty()) {
                    owner = users.get(0);
                }
            }
            if (owner == null) {
                owner = currentUser != null ? currentUser : userRepository.findAll().stream().findFirst().orElse(null);
            }
        }

        // 2. Resolve Contact
        Contact contact = null;
        if (req.getContactId() != null && !req.getContactId().isBlank()) {
            try {
                contact = contactService.getContactByIdOrCode(req.getContactId());
            } catch (Exception ignored) {}
        }
        if (contact == null && req.getContactName() != null && !req.getContactName().isBlank()) {
            List<Contact> foundContacts = contactRepository.searchContacts(req.getContactName().trim());
            if (!foundContacts.isEmpty()) {
                contact = foundContacts.get(0);
            }
        }
        if (contact == null) {
            if (req.getContactName() != null && !req.getContactName().isBlank()) {
                String full = req.getContactName().trim();
                String first = full;
                String last = "";
                int lastSpace = full.lastIndexOf(' ');
                if (lastSpace > 0) {
                    first = full.substring(0, lastSpace);
                    last = full.substring(lastSpace + 1);
                }
                contact = contactRepository.save(Contact.builder()
                        .firstName(first)
                        .lastName(last)
                        .email(req.getContactEmail() != null && !req.getContactEmail().isBlank() ? req.getContactEmail() : "client@insurmatch.us")
                        .phone(req.getContactPhone() != null && !req.getContactPhone().isBlank() ? req.getContactPhone() : "+1 (832) 555-0199")
                        .contactOwner(owner)
                        .supportAgent(owner)
                        .build());
            } else {
                List<Contact> allContacts = contactRepository.findAll();
                if (!allContacts.isEmpty()) {
                    contact = allContacts.get(0);
                } else {
                    contact = contactRepository.save(Contact.builder()
                            .firstName("Hai")
                            .lastName("Nguyen")
                            .email("client@insurmatch.us")
                            .phone("+1 (832) 555-0199")
                            .contactOwner(owner)
                            .supportAgent(owner)
                            .build());
                }
            }
        }

        // 3. Generate Title & Code
        String finalTitle = req.getDealName() != null && !req.getDealName().isBlank()
                ? req.getDealName()
                : (req.getTitle() != null && !req.getTitle().isBlank()
                ? req.getTitle()
                : (contact.getFullName() + " - " + (req.getPipeline() != null ? req.getPipeline() : "Obamacare 2026")));

        String newCode = "D2600" + (5000 + RANDOM.nextInt(900));

        boolean isUploadNeeded = "Yes".equalsIgnoreCase(req.getNeedUpload())
                || Boolean.TRUE.equals(req.getUploadRequest());

        LocalDate bed = null;
        if (req.getBrokerEffectiveDate() != null && !req.getBrokerEffectiveDate().isBlank()) {
            try {
                bed = LocalDate.parse(req.getBrokerEffectiveDate());
            } catch (Exception ignored) {}
        }

        Deal deal = Deal.builder()
                .code(newCode)
                .dealName(finalTitle)
                .pipeline(req.getPipeline() != null && !req.getPipeline().equals("--") ? req.getPipeline() : "Obamacare 2026")
                .dealStage(req.getStage() != null && !req.getStage().equals("--")
                        ? req.getStage()
                        : (req.getDealStage() != null ? req.getDealStage() : "Ready to Enroll (Obamacare 2026)"))
                .carrier(req.getCarrier() != null && !req.getCarrier().equals("--") ? req.getCarrier() : "BCBS")
                .sellingState(req.getSellingState() != null && !req.getSellingState().equals("--") ? req.getSellingState() : "North Carolina (NC)")
                .planName(req.getPlanName() != null ? req.getPlanName() : "")
                .amount(req.getAmount())
                .closeDate(req.getCloseDate() != null ? req.getCloseDate() : "_ _ _ _ _ _ _ _ _ _")
                .contact(contact)
                .member(req.getMember() != null && !req.getMember().equals("--") ? req.getMember() : contact.getFullName())
                .primaryMemberId(req.getPrimaryMemberId() != null ? req.getPrimaryMemberId() : "")
                .numberMember(req.getNumberMember() != null ? req.getNumberMember() : 1)
                .applicationId(req.getApplicationId() != null ? req.getApplicationId() : "")
                .estimatedIncome(req.getEstimateHouseholdIncome())
                .householdSize(req.getHouseholdMember())
                .enrolledAddress(req.getEnrolledAddress() != null ? req.getEnrolledAddress() : "")
                .quotedCounty(req.getQuotedCounty() != null ? req.getQuotedCounty() : "")
                .isBackdateDeal(req.getIsBackdateDeal() != null ? req.getIsBackdateDeal() : "No")
                .uploadRequest(isUploadNeeded)
                .enrolledNpn(req.getEnrolledNpn() != null ? req.getEnrolledNpn() : "")
                .brokerEffectiveDate(bed)
                .saleSupportStatus(req.getSaleSupportStatus() != null ? req.getSaleSupportStatus() : "None")
                .monthlyPremium(req.getMonthlyPremium())
                .subsidyAmount(req.getSubsidyAmount())
                .agencyCommission(req.getAgencyCommission())
                .bonusTier(req.getBonusTier() != null ? req.getBonusTier() : "Standard Tier")
                .paymentOption(req.getPaymentOption())
                .paymentVerification(req.getPaymentVerification())
                .contactPhone(req.getContactPhone())
                .contactEmail(req.getContactEmail())
                .dealOwner(owner)
                .supportAgent(owner)
                .build();

        Deal savedDeal = dealRepository.save(deal);

        // 4. AUTOMATION: Auto-generate Upload Document Ticket if needUpload = Yes
        if (isUploadNeeded) {
            Ticket uploadTicket = Ticket.builder()
                    .ticketName("Upload documents - " + finalTitle)
                    .pipeline("Upload document")
                    .ticketStatus("Open")
                    .priority("HIGH")
                    .dueDate(LocalDate.now().plusDays(3))
                    .ticketDescription("Tự động tạo từ Deal yêu cầu nộp tài liệu bổ sung (Proof of income, immigration documents)")
                    .contact(contact)
                    .deal(savedDeal)
                    .ticketOwner(owner)
                    .serviceAgent(owner)
                    .build();
            ticketRepository.save(uploadTicket);
            log.info("🎉 Auto-generated Upload Document ticket for deal: {}", savedDeal.getCode());
        }

        // 5. AUTOMATION: Log Activity Timeline
        String nowStr = LocalDateTime.now().format(TIME_FORMATTER);
        Activity activity = Activity.builder()
                .type("Deal Created")
                .actor("Platform Staff")
                .time(nowStr)
                .summary("Created deal: " + finalTitle + (isUploadNeeded ? " (kèm yêu cầu upload giấy tờ)" : ""))
                .dealTitle(finalTitle)
                .deal(savedDeal)
                .contact(contact)
                .build();
        activityRepository.save(activity);

        return getDealResponseById(savedDeal.getId());
    }

    @Transactional
    public DealResponse updateDeal(Long id, CreateDealRequest req) {
        Deal deal = getDealById(id);
        User currentUser = getCurrentAuthenticatedUser();

        // RBAC: Check edit permission & Reassignment rights
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            if (deal.getDealOwner() == null || !deal.getDealOwner().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Bạn chỉ có quyền cập nhật hợp đồng do mình phụ trách!");
            }
            // Agent CANNOT reassign deal owner - dealOwner remains unchanged
        } else {
            // ADMIN / STAFF can assign & reassign deal owner
            if (req.getDealOwnerId() != null) {
                userRepository.findById(req.getDealOwnerId()).ifPresent(deal::setDealOwner);
            } else if (req.getDealOwner() != null && !req.getDealOwner().isBlank() && !req.getDealOwner().equals("--")) {
                List<User> users = userRepository.searchByNameOrEmail(req.getDealOwner());
                if (!users.isEmpty()) {
                    deal.setDealOwner(users.get(0));
                }
            }
        }

        String oldStage = deal.getDealStage();

        if (req.getDealName() != null && !req.getDealName().isBlank()) deal.setDealName(req.getDealName());
        if (req.getTitle() != null && !req.getTitle().isBlank()) deal.setDealName(req.getTitle());
        if (req.getPipeline() != null && !req.getPipeline().isBlank()) deal.setPipeline(req.getPipeline());

        String newStage = req.getStage() != null ? req.getStage() : req.getDealStage();
        if (newStage != null && !newStage.isBlank() && !newStage.equals(oldStage)) {
            deal.setDealStage(newStage);
            // Log stage change activity
            String nowStr = LocalDateTime.now().format(TIME_FORMATTER);
            activityRepository.save(Activity.builder()
                    .type("Deal Activity")
                    .actor(currentUser != null ? currentUser.getName() : "Platform Staff")
                    .time(nowStr)
                    .summary("moved deal stage from \"" + oldStage + "\" to \"" + newStage + "\"")
                    .dealTitle(deal.getDealName())
                    .deal(deal)
                    .contact(deal.getContact())
                    .build());
        }

        if (req.getCarrier() != null) deal.setCarrier(req.getCarrier());
        if (req.getSellingState() != null) deal.setSellingState(req.getSellingState());
        if (req.getPlanName() != null) deal.setPlanName(req.getPlanName());
        if (req.getAmount() != null) deal.setAmount(req.getAmount());
        if (req.getCloseDate() != null) deal.setCloseDate(req.getCloseDate());
        if (req.getMember() != null) deal.setMember(req.getMember());
        if (req.getPrimaryMemberId() != null) deal.setPrimaryMemberId(req.getPrimaryMemberId());
        if (req.getNumberMember() != null) deal.setNumberMember(req.getNumberMember());
        if (req.getApplicationId() != null) deal.setApplicationId(req.getApplicationId());
        if (req.getEstimateHouseholdIncome() != null) deal.setEstimatedIncome(req.getEstimateHouseholdIncome());
        if (req.getHouseholdMember() != null) deal.setHouseholdSize(req.getHouseholdMember());
        if (req.getEnrolledAddress() != null) deal.setEnrolledAddress(req.getEnrolledAddress());
        if (req.getQuotedCounty() != null) deal.setQuotedCounty(req.getQuotedCounty());
        if (req.getIsBackdateDeal() != null) deal.setIsBackdateDeal(req.getIsBackdateDeal());
        if (req.getNeedUpload() != null) deal.setUploadRequest("Yes".equalsIgnoreCase(req.getNeedUpload()));
        if (req.getEnrolledNpn() != null) deal.setEnrolledNpn(req.getEnrolledNpn());
        if (req.getSaleSupportStatus() != null) deal.setSaleSupportStatus(req.getSaleSupportStatus());
        if (req.getMonthlyPremium() != null) deal.setMonthlyPremium(req.getMonthlyPremium());
        if (req.getSubsidyAmount() != null) deal.setSubsidyAmount(req.getSubsidyAmount());
        if (req.getAgencyCommission() != null) deal.setAgencyCommission(req.getAgencyCommission());
        if (req.getBonusTier() != null) deal.setBonusTier(req.getBonusTier());
        if (req.getPaymentOption() != null) deal.setPaymentOption(req.getPaymentOption());
        if (req.getPaymentVerification() != null) deal.setPaymentVerification(req.getPaymentVerification());
        if (req.getContactPhone() != null) deal.setContactPhone(req.getContactPhone());
        if (req.getContactEmail() != null) deal.setContactEmail(req.getContactEmail());

        if (req.getBrokerEffectiveDate() != null && !req.getBrokerEffectiveDate().isBlank()) {
            try {
                deal.setBrokerEffectiveDate(LocalDate.parse(req.getBrokerEffectiveDate()));
            } catch (Exception ignored) {}
        }

        Deal updated = dealRepository.save(deal);
        return getDealResponseById(updated.getId());
    }

    public DealResponse updateDeal(String identifier, CreateDealRequest req) {
        return updateDeal(getDealByIdOrCode(identifier).getId(), req);
    }

    @Transactional
    public DealResponse updateStage(Long id, UpdateStageRequest req) {
        Deal deal = getDealById(id);
        User currentUser = getCurrentAuthenticatedUser();

        // RBAC: Check edit permission
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            if (deal.getDealOwner() == null || !deal.getDealOwner().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Bạn chỉ có quyền chuyển trạng thái hợp đồng do mình phụ trách!");
            }
        }

        String oldStage = deal.getDealStage();
        String newStage = req.getStage();

        if (newStage != null && !newStage.equals(oldStage)) {
            deal.setDealStage(newStage);
            String actor = req.getUser() != null && !req.getUser().isBlank() 
                    ? req.getUser() 
                    : (currentUser != null ? currentUser.getName() : "Platform Staff");
            String nowStr = LocalDateTime.now().format(TIME_FORMATTER);

            activityRepository.save(Activity.builder()
                    .type("Deal Activity")
                    .actor(actor)
                    .time(nowStr)
                    .summary("moved deal stage from \"" + oldStage + "\" to \"" + newStage + "\"")
                    .dealTitle(deal.getDealName())
                    .deal(deal)
                    .contact(deal.getContact())
                    .build());

            dealRepository.save(deal);
        }

        return getDealResponseById(deal.getId());
    }

    public DealResponse updateStage(String identifier, UpdateStageRequest req) {
        return updateStage(getDealByIdOrCode(identifier).getId(), req);
    }

    @Transactional
    public Note addNote(Long dealId, Note note) {
        Deal deal = getDealById(dealId);
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            if (deal.getDealOwner() == null || !deal.getDealOwner().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Bạn chỉ có quyền thêm ghi chú vào hợp đồng do mình phụ trách!");
            }
        }
        note.setDeal(deal);
        if (deal.getContact() != null) {
            note.setContact(deal.getContact());
        }
        return noteRepository.save(note);
    }

    public Note addNote(String identifier, Note note) {
        return addNote(getDealByIdOrCode(identifier).getId(), note);
    }

    @Transactional
    public Task addTask(Long dealId, Task task) {
        Deal deal = getDealById(dealId);
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            if (deal.getDealOwner() == null || !deal.getDealOwner().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Bạn chỉ có quyền thêm công việc vào hợp đồng do mình phụ trách!");
            }
        }
        task.setDeal(deal);
        if (deal.getContact() != null) {
            task.setContact(deal.getContact());
        }
        return taskRepository.save(task);
    }

    public Task addTask(String identifier, Task task) {
        return addTask(getDealByIdOrCode(identifier).getId(), task);
    }

    public List<Activity> getActivities(Long dealId) {
        return activityRepository.findByDealIdOrderByCreatedAtDesc(dealId);
    }

    public List<Activity> getActivities(String identifier) {
        return getActivities(getDealByIdOrCode(identifier).getId());
    }

    public long count() {
        return dealRepository.count();
    }
}
