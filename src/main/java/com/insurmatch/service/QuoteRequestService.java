package com.insurmatch.service;

import com.insurmatch.entity.QuoteRequest;
import com.insurmatch.entity.User;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.QuoteRequestRepository;
import com.insurmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuoteRequestService {

    private final QuoteRequestRepository quoteRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public List<QuoteRequest> getAllQuotes() {
        return quoteRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public QuoteRequest getQuoteById(Long id) {
        return quoteRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QuoteRequest not found with id: " + id));
    }

    public QuoteRequest createQuote(QuoteRequest quote) {
        quote.ensureFullNameAndNotes();
        quote.setStatus("NEW");
        QuoteRequest saved = quoteRequestRepository.save(quote);

        // Tự động khởi tạo tài khoản Agent dùng thử 60 ngày nếu email chưa tồn tại trong hệ thống
        if (quote.getEmail() != null && !quote.getEmail().trim().isEmpty()) {
            String normalizedEmail = quote.getEmail().trim().toLowerCase();
            String fullName = quote.getFullName() != null && !quote.getFullName().trim().isEmpty()
                    ? quote.getFullName().trim()
                    : "Trial Agent";

            if (!userRepository.existsByEmail(normalizedEmail)) {
                String[] parts = fullName.split("\\s+", 2);
                String fName = parts[0];
                String lName = parts.length > 1 ? parts[1] : "Agent";

                User trialUser = User.builder()
                        .email(normalizedEmail)
                        .password(passwordEncoder.encode("Agent@123"))
                        .firstName(fName)
                        .lastName(lName)
                        .phone(quote.getPhone())
                        .role(User.Role.AGENT)
                        .statesLicensed(quote.getState() != null && !quote.getState().trim().isEmpty() ? quote.getState() : "TX")
                        .department("Trial Workspace (60 Days)")
                        .status("Active")
                        .complianceStatus("Verified & Cleared")
                        .active(true)
                        .emailVerified(true)
                        .build();
                userRepository.save(trialUser);
            }

            // Gửi email chào mừng kèm thông tin tài khoản và mật khẩu đăng nhập trực tiếp về hộp thư
            try {
                emailService.sendWelcomeAccountEmail(
                        normalizedEmail,
                        fullName,
                        "Agent@123",
                        "Agent (Dùng thử miễn phí 60 ngày)"
                );
            } catch (Exception e) {
                // Non-blocking: ghi log và tiếp tục
            }
        }

        return saved;
    }

    public QuoteRequest assignAgent(Long quoteId, Long agentId) {
        QuoteRequest quote = getQuoteById(quoteId);
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + agentId));
        quote.setAssignedAgent(agent);
        quote.setStatus("ASSIGNED");
        return quoteRequestRepository.save(quote);
    }

    public long count() {
        return quoteRequestRepository.count();
    }

    public long countByStatus(String status) {
        return quoteRequestRepository.findByStatus(status).size();
    }
}
