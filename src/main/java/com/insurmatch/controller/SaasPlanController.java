package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.SaasPlan;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.SaasPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/saas-plans", "/saas-plans"})
@RequiredArgsConstructor
public class SaasPlanController {

    private final SaasPlanRepository saasPlanRepository;

    /**
     * 1. Lấy danh sách toàn bộ các gói SaaS đang hoạt động (Public API)
     * GET /api/saas-plans
     */
    @GetMapping({"", "/"})
    public ResponseEntity<ApiResponse<List<SaasPlan>>> getActivePlans() {
        // Tự động khởi tạo dữ liệu mẫu nếu bảng trong DB đang rỗng
        if (saasPlanRepository.count() == 0) {
            initDefaultSaasPlans();
        }
        List<SaasPlan> plans = saasPlanRepository.findByIsActiveTrueOrderByMonthlyPriceVndAsc();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    /**
     * 2. Lấy chi tiết một gói theo mã (TRIAL, STARTER, PROFESSIONAL, AGENCY)
     * GET /api/saas-plans/{code}
     */
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<SaasPlan>> getPlanByCode(@PathVariable String code) {
        SaasPlan plan = saasPlanRepository.findByPlanCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói: " + code));
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    /**
     * 3. Dành cho Admin: Cập nhật giá tiền hoặc hạn mức của một gói
     * PUT /api/admin/saas-plans/{id}
     */
    @PutMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<SaasPlan>> updatePlan(@PathVariable Long id, @RequestBody SaasPlan updatedData) {
        SaasPlan existing = saasPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói ID: " + id));

        if (updatedData.getMonthlyPriceVnd() != null) existing.setMonthlyPriceVnd(updatedData.getMonthlyPriceVnd());
        if (updatedData.getAnnualPriceVnd() != null) existing.setAnnualPriceVnd(updatedData.getAnnualPriceVnd());
        if (updatedData.getMonthlyPriceUsd() != null) existing.setMonthlyPriceUsd(updatedData.getMonthlyPriceUsd());
        if (updatedData.getAnnualPriceUsd() != null) existing.setAnnualPriceUsd(updatedData.getAnnualPriceUsd());
        if (updatedData.getMaxContacts() != null) existing.setMaxContacts(updatedData.getMaxContacts());
        if (updatedData.getMaxSeats() != null) existing.setMaxSeats(updatedData.getMaxSeats());
        if (updatedData.getStorageLimitGb() != null) existing.setStorageLimitGb(updatedData.getStorageLimitGb());
        if (updatedData.getName() != null) existing.setName(updatedData.getName());
        if (updatedData.getTargetAudience() != null) existing.setTargetAudience(updatedData.getTargetAudience());
        if (updatedData.getDescription() != null) existing.setDescription(updatedData.getDescription());
        if (updatedData.getIsPopular() != null) existing.setIsPopular(updatedData.getIsPopular());
        if (updatedData.getIsActive() != null) existing.setIsActive(updatedData.getIsActive());

        SaasPlan saved = saasPlanRepository.save(existing);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật gói thành công!", saved));
    }

    /**
     * Khởi tạo sẵn 4 gói chuẩn vào DB
     */
    public void initDefaultSaasPlans() {
        log.info("Bảng saas_plans đang trống. Đang tự động nạp 4 gói dịch vụ chuẩn vào Database Neon...");

        SaasPlan trial = SaasPlan.builder()
                .planCode("TRIAL")
                .name("Trial 60 Ngày")
                .targetAudience("Dùng thử miễn phí")
                .monthlyPriceVnd(0L)
                .annualPriceVnd(0L)
                .monthlyPriceUsd(0)
                .annualPriceUsd(0)
                .maxSeats(1)
                .maxContacts(50)
                .storageLimitGb(1)
                .isPopular(false)
                .description("Trải nghiệm đầy đủ tính năng CRM trong 60 ngày hoàn toàn miễn phí không cần thẻ tín dụng.")
                .featuresJson("[\"1 Tài khoản Agent\",\"Tối đa 50 Hồ sơ mẫu\",\"Pipelines ACA & Medicare cơ bản\",\"Cảnh báo Turning 65 & Gia hạn ACA\",\"Miễn phí 60 ngày ($0)\"]")
                .isActive(true)
                .build();

        SaasPlan starter = SaasPlan.builder()
                .planCode("STARTER")
                .name("Starter")
                .targetAudience("Cá nhân đại lý (Solopreneur)")
                .monthlyPriceVnd(990_000L)
                .annualPriceVnd(9_900_000L) // Tiết kiệm 2 tháng
                .monthlyPriceUsd(39)
                .annualPriceUsd(32)
                .maxSeats(1)
                .maxContacts(100)
                .storageLimitGb(2)
                .isPopular(false)
                .description("Dành cho cá nhân đại lý độc lập quản lý danh bạ khách hàng thay cho bảng tính Excel.")
                .featuresJson("[\"1 Tài khoản Agent (Cá nhân)\",\"Hạn mức: 100 Hồ sơ Khách hàng\",\"Theo dõi Deal ACA & Medicare chuẩn\",\"Lịch hẹn, ghi chú & nhắc việc tự động\",\"Cảnh báo Turning 65 & Gia hạn ACA\",\"Lưu trữ tài liệu: 2 GB\",\"Thanh toán VietQR PayOS: 990.000đ/tháng\"]")
                .isActive(true)
                .build();

        SaasPlan pro = SaasPlan.builder()
                .planCode("PROFESSIONAL")
                .name("Professional")
                .targetAudience("Đội nhóm nhỏ (Small Team)")
                .monthlyPriceVnd(1_990_000L)
                .annualPriceVnd(19_900_000L)
                .monthlyPriceUsd(79)
                .annualPriceUsd(65)
                .maxSeats(3)
                .maxContacts(200)
                .storageLimitGb(25)
                .isPopular(true)
                .description("Thiết kế cho nhóm đại lý cần tùy chỉnh quy trình Pipeline, tự động nhắc việc SLA 48h.")
                .featuresJson("[\"Tối đa 3 Agent Seats cho đội nhóm\",\"Hạn mức: 200 Hồ sơ Khách hàng\",\"Tùy chỉnh Stage & Pipeline riêng\",\"Nhắc việc tự động theo SLA 48h\",\"Bảng đối soát hoa hồng 100% từ hãng\",\"Lưu trữ tài liệu: 25 GB\",\"Thanh toán VietQR PayOS: 1.990.000đ/tháng\"]")
                .isActive(true)
                .build();

        SaasPlan agency = SaasPlan.builder()
                .planCode("AGENCY")
                .name("Agency")
                .targetAudience("Văn phòng tổng đại lý (Multi-Agent Agency)")
                .monthlyPriceVnd(4_990_000L)
                .annualPriceVnd(49_900_000L)
                .monthlyPriceUsd(199)
                .annualPriceUsd(165)
                .maxSeats(10)
                .maxContacts(300)
                .storageLimitGb(100)
                .isPopular(false)
                .description("Hạ tầng quản lý toàn diện: Tự động chia Lead, giám sát hiệu suất và báo cáo doanh thu.")
                .featuresJson("[\"Tối đa 10 Seats phân quyền Manager/Staff/Agent\",\"Hạn mức: 300 Hồ sơ Khách hàng\",\"Lead Auto-routing tự động chia việc\",\"Master Commission Ledger toàn sàn\",\"Dung lượng tài liệu: 100 GB\",\"Báo cáo hiệu suất toàn Agency\",\"Thanh toán VietQR PayOS: 4.990.000đ/tháng\"]")
                .isActive(true)
                .build();

        saasPlanRepository.saveAll(List.of(trial, starter, pro, agency));
        log.info("Đã khởi tạo thành công 4 gói dịch vụ vào bảng saas_plans!");
    }
}

