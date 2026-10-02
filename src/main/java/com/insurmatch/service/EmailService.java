package com.insurmatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.username:noreply@insurmatch.us}")
    private String fromEmail;

    @Value("${app.frontend.url:https://thebestrateins-exe201.vercel.app}")
    private String frontendUrl;

    @Value("${resend.api-key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${app.mail.sender-email:}")
    private String customSenderEmail;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isMailConfigured() {
        return mailSender != null
                && mailUsername != null && !mailUsername.trim().isEmpty()
                && mailPassword != null && !mailPassword.trim().isEmpty();
    }

    private String getFromAddress() {
        if (customSenderEmail != null && !customSenderEmail.trim().isEmpty()) {
            return customSenderEmail.trim();
        }
        if (mailUsername != null && !mailUsername.trim().isEmpty()) {
            return mailUsername.trim();
        }
        return fromEmail;
    }

    /**
     * Gửi email thống nhất:
     * 1. Ưu tiên Resend HTTP API (Port 443 — Không bao giờ bị chặn trên Render Free)
     * 2. Ưu tiên Brevo HTTP API (Port 443 — Miễn phí 300 mail/ngày)
     * 3. Fallback SMTP truyền thống (khi chạy local hoặc Render có mở cổng)
     */
    public boolean sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        if (resendApiKey != null && !resendApiKey.trim().isEmpty()) {
            log.info("Sending email to {} via Resend HTTP API (Port 443)...", toEmail);
            if (sendViaResend(toEmail, subject, htmlContent)) {
                return true;
            }
        }

        if (brevoApiKey != null && !brevoApiKey.trim().isEmpty()) {
            log.info("Sending email to {} via Brevo HTTP API (Port 443)...", toEmail);
            if (sendViaBrevo(toEmail, subject, htmlContent)) {
                return true;
            }
        }

        if (isMailConfigured()) {
            log.info("Sending email to {} via JavaMail SMTP...", toEmail);
            return sendViaSmtp(toEmail, subject, htmlContent);
        }

        log.info("No email API Key (RESEND_API_KEY / BREVO_API_KEY) configured. Credentials logged to console.");
        return true;
    }

    private boolean sendViaResend(String toEmail, String subject, String htmlContent) {
        try {
            String from = customSenderEmail != null && !customSenderEmail.trim().isEmpty()
                    ? customSenderEmail.trim()
                    : "InsurMatch <onboarding@resend.dev>";

            Map<String, Object> body = new HashMap<>();
            body.put("from", from);
            body.put("to", List.of(toEmail));
            body.put("subject", subject);
            body.put("html", htmlContent);

            String jsonPayload = objectMapper.writeValueAsString(body);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully sent email via Resend API to {}. Response: {}", toEmail, response.body());
                return true;
            } else {
                log.warn("Resend API failed with status {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.warn("Error sending email via Resend API to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    private boolean sendViaBrevo(String toEmail, String subject, String htmlContent) {
        try {
            String senderEmail = getFromAddress();

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", "InsurMatch Platform", "email", senderEmail));
            body.put("to", List.of(Map.of("email", toEmail)));
            body.put("subject", subject);
            body.put("htmlContent", htmlContent);

            String jsonPayload = objectMapper.writeValueAsString(body);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully sent email via Brevo API to {}. Response: {}", toEmail, response.body());
                return true;
            } else {
                log.warn("Brevo API failed with status {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.warn("Error sending email via Brevo API to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    private boolean sendViaSmtp(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(getFromAddress(), "InsurMatch Platform");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Successfully sent email via SMTP to {}", toEmail);
            return true;
        } catch (Exception e) {
            log.warn("SMTP sending to {} failed: {}", toEmail, e.getMessage());
            return false;
        }
    }

    /**
     * Gửi email mã OTP xác thực tài khoản.
     * Tự động fallback in ra Console Log nếu chưa cấu hình Mail Server thật.
     */
    public boolean sendOtpEmail(String toEmail, String otp, String recipientName) {
        logOtpToConsole(toEmail, otp, recipientName);
        String subject = "🛡️ [InsurMatch] Mã xác thực OTP đăng ký tài khoản: " + otp;
        String htmlContent = buildOtpHtmlContent(recipientName, otp);
        return sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Gửi email mã OTP khôi phục / đặt lại mật khẩu.
     */
    public boolean sendResetPasswordEmail(String toEmail, String otp, String recipientName) {
        log.info("\n" +
                "================================================================================\n" +
                "🔑 [INSURMATCH EMAIL SERVICE — PASSWORD RESET OTP]\n" +
                "--------------------------------------------------------------------------------\n" +
                "To:        {} ({})\n" +
                "OTP Code:  {}\n" +
                "Expires:   5 minutes\n" +
                "Action:    Reset password at POST /api/auth/reset-password\n" +
                "================================================================================",
                toEmail, recipientName != null ? recipientName : "User", otp);

        String subject = "🔑 [InsurMatch] Yêu cầu đặt lại mật khẩu: " + otp;
        String htmlContent = buildResetPasswordHtmlContent(recipientName, otp);
        return sendHtmlEmail(toEmail, subject, htmlContent);
    }

    private void logOtpToConsole(String toEmail, String otp, String name) {
        log.info("\n" +
                "================================================================================\n" +
                "🔐 [INSURMATCH EMAIL SERVICE — OTP VERIFICATION]\n" +
                "--------------------------------------------------------------------------------\n" +
                "To:        {} ({})\n" +
                "OTP Code:  {}\n" +
                "Expires:   5 minutes\n" +
                "Action:    Verify your account at POST /api/auth/verify-otp\n" +
                "================================================================================",
                toEmail, name != null ? name : "User", otp);
    }

    private String buildOtpHtmlContent(String name, String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='margin:0;padding:0;background-color:#0B172A;font-family:Arial,sans-serif;'>" +
                "  <table width='100%' border='0' cellspacing='0' cellpadding='0' style='background-color:#0B172A;padding:40px 10px;'>" +
                "    <tr>" +
                "      <td align='center'>" +
                "        <table width='600' border='0' cellspacing='0' cellpadding='0' style='background-color:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 8px 30px rgba(0,0,0,0.3);'>" +
                "          <!-- Header -->" +
                "          <tr>" +
                "            <td style='background-color:#0B172A;padding:32px 40px;text-align:center;border-bottom:3px solid #C8A96B;'>" +
                "              <h1 style='color:#FFFFFF;margin:0;font-size:24px;letter-spacing:1px;'>INSUR<span style='color:#C8A96B;'>MATCH</span></h1>" +
                "              <p style='color:#94A3B8;margin:6px 0 0 0;font-size:12px;text-transform:uppercase;letter-spacing:2px;'>Insurance CRM & Marketplace</p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Body -->" +
                "          <tr>" +
                "            <td style='padding:40px;'>" +
                "              <h2 style='color:#0F172A;margin-top:0;font-size:20px;'>Xác thực đăng ký tài khoản</h2>" +
                "              <p style='color:#475569;font-size:15px;line-height:1.6;margin-bottom:24px;'>" +
                "                Xin chào <strong>" + (name != null ? name : "Quý khách") + "</strong>,<br>" +
                "                Cảm ơn bạn đã đăng ký tài khoản trên nền tảng <strong>InsurMatch</strong>. Vui lòng sử dụng mã OTP dưới đây để hoàn tất kích hoạt tài khoản của bạn:" +
                "              </p>" +
                "              <!-- OTP Box -->" +
                "              <div style='background:linear-gradient(135deg, #0B172A 0%, #1E293B 100%);border-radius:12px;padding:24px;text-align:center;margin:28px 0;'>" +
                "                <span style='color:#94A3B8;font-size:12px;display:block;letter-spacing:1px;text-transform:uppercase;margin-bottom:8px;'>MÃ XÁC THỰC (OTP) CỦA BẠN</span>" +
                "                <span style='color:#C8A96B;font-size:36px;font-weight:bold;letter-spacing:8px;font-family:monospace;'>" + otp + "</span>" +
                "              </div>" +
                "              <p style='color:#64748B;font-size:13px;line-height:1.5;'>" +
                "                ⏱️ Mã này có hiệu lực trong vòng <strong>5 phút</strong>.<br>" +
                "                ⚠️ Tuyệt đối không chia sẻ mã OTP này với bất kỳ ai để đảm bảo an toàn cho tài khoản." +
                "              </p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Footer -->" +
                "          <tr>" +
                "            <td style='background-color:#F8FAFC;padding:24px;text-align:center;border-top:1px solid #E2E8F0;color:#94A3B8;font-size:12px;'>" +
                "              © 2026 InsurMatch Platform. All rights reserved.<br>" +
                "              Hệ thống quản trị và phân phối bảo hiểm chuyên nghiệp." +
                "            </td>" +
                "          </tr>" +
                "        </table>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }

    private String buildResetPasswordHtmlContent(String name, String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='margin:0;padding:0;background-color:#0B172A;font-family:Arial,sans-serif;'>" +
                "  <table width='100%' border='0' cellspacing='0' cellpadding='0' style='background-color:#0B172A;padding:40px 10px;'>" +
                "    <tr>" +
                "      <td align='center'>" +
                "        <table width='600' border='0' cellspacing='0' cellpadding='0' style='background-color:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 8px 30px rgba(0,0,0,0.3);'>" +
                "          <!-- Header -->" +
                "          <tr>" +
                "            <td style='background-color:#0B172A;padding:32px 40px;text-align:center;border-bottom:3px solid #EF4444;'>" +
                "              <h1 style='color:#FFFFFF;margin:0;font-size:24px;letter-spacing:1px;'>INSUR<span style='color:#C8A96B;'>MATCH</span></h1>" +
                "              <p style='color:#94A3B8;margin:6px 0 0 0;font-size:12px;text-transform:uppercase;letter-spacing:2px;'>Bảo Mật & Khôi Phục Tài Khoản</p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Body -->" +
                "          <tr>" +
                "            <td style='padding:40px;'>" +
                "              <h2 style='color:#0F172A;margin-top:0;font-size:20px;'>Yêu cầu đặt lại mật khẩu</h2>" +
                "              <p style='color:#475569;font-size:15px;line-height:1.6;margin-bottom:24px;'>" +
                "                Xin chào <strong>" + (name != null ? name : "Quý khách") + "</strong>,<br>" +
                "                Chúng tôi vừa nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn trên <strong>InsurMatch</strong>. Vui lòng nhập mã OTP dưới đây để hoàn tất việc thiết lập mật khẩu mới:" +
                "              </p>" +
                "              <!-- OTP Box -->" +
                "              <div style='background:linear-gradient(135deg, #0B172A 0%, #1E293B 100%);border-radius:12px;padding:24px;text-align:center;margin:28px 0;border:1px solid #EF4444;'>" +
                "                <span style='color:#F87171;font-size:12px;display:block;letter-spacing:1px;text-transform:uppercase;margin-bottom:8px;'>MÃ OTP ĐẶT LẠI MẬT KHẨU</span>" +
                "                <span style='color:#FFFFFF;font-size:36px;font-weight:bold;letter-spacing:8px;font-family:monospace;'>" + otp + "</span>" +
                "              </div>" +
                "              <p style='color:#64748B;font-size:13px;line-height:1.5;'>" +
                "                ⏱️ Mã này có hiệu lực trong vòng <strong>5 phút</strong>.<br>" +
                "                ⚠️ Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc liên hệ với bộ phận an ninh của chúng tôi ngay lập tức." +
                "              </p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Footer -->" +
                "          <tr>" +
                "            <td style='background-color:#F8FAFC;padding:24px;text-align:center;border-top:1px solid #E2E8F0;color:#94A3B8;font-size:12px;'>" +
                "              © 2026 InsurMatch Platform. All rights reserved.<br>" +
                "              Hệ thống bảo vệ tài khoản người dùng tự động." +
                "            </td>" +
                "          </tr>" +
                "        </table>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }

    /**
     * Gửi email chào mừng và bàn giao thông tin đăng nhập Portal cho Agent/Staff mới do Admin tạo.
     */
    public boolean sendWelcomeAccountEmail(String toEmail, String recipientName, String temporaryPassword, String roleName) {
        String portalUrl = frontendUrl != null ? frontendUrl + "/login" : "https://thebestrateins-exe201.vercel.app/login";

        log.info("\n" +
                "================================================================================\n" +
                "🎉 [INSURMATCH EMAIL SERVICE — WELCOME NEW MEMBER]\n" +
                "--------------------------------------------------------------------------------\n" +
                "To:                 {} ({})\n" +
                "Role:               {}\n" +
                "Initial Password:   {}\n" +
                "Portal URL:         {}\n" +
                "================================================================================",
                toEmail, recipientName != null ? recipientName : "Member", roleName, temporaryPassword, portalUrl);

        String subject = "🎉 [InsurMatch] Thông tin tài khoản cổng đối tác của bạn";
        String htmlContent = buildWelcomeAccountHtmlContent(recipientName, toEmail, temporaryPassword, roleName);
        return sendHtmlEmail(toEmail, subject, htmlContent);
    }

    private String buildWelcomeAccountHtmlContent(String name, String email, String password, String role) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='margin:0;padding:0;background-color:#0B172A;font-family:Arial,sans-serif;'>" +
                "  <table width='100%' border='0' cellspacing='0' cellpadding='0' style='background-color:#0B172A;padding:40px 10px;'>" +
                "    <tr>" +
                "      <td align='center'>" +
                "        <table width='600' border='0' cellspacing='0' cellpadding='0' style='background-color:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 8px 30px rgba(0,0,0,0.3);'>" +
                "          <!-- Header -->" +
                "          <tr>" +
                "            <td style='background-color:#0B172A;padding:32px 40px;text-align:center;border-bottom:3px solid #C8A96B;'>" +
                "              <h1 style='color:#FFFFFF;margin:0;font-size:24px;letter-spacing:1px;'>INSUR<span style='color:#C8A96B;'>MATCH</span></h1>" +
                "              <p style='color:#94A3B8;margin:6px 0 0 0;font-size:12px;text-transform:uppercase;letter-spacing:2px;'>Partner Portal Access</p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Body -->" +
                "          <tr>" +
                "            <td style='padding:40px;'>" +
                "              <h2 style='color:#0F172A;margin-top:0;font-size:20px;'>Chào mừng bạn gia nhập mạng lưới InsurMatch!</h2>" +
                "              <p style='color:#475569;font-size:15px;line-height:1.6;margin-bottom:20px;'>" +
                "                Xin chào <strong>" + (name != null ? name : "Quý đối tác") + "</strong>,<br>" +
                "                Quản trị viên đã khởi tạo thành công tài khoản thành viên của bạn trên nền tảng <strong>InsurMatch CRM</strong> với vai trò: <strong style='color:#0B172A;text-transform:uppercase;'>" + role + "</strong>." +
                "              </p>" +
                "              <!-- Credentials Box -->" +
                "              <div style='background-color:#F8FAFC;border:1px solid #E2E8F0;border-radius:12px;padding:20px;margin:24px 0;'>" +
                "                <div style='margin-bottom:10px;font-size:14px;color:#334155;'>" +
                "                  <strong>Tài khoản đăng nhập:</strong> <span style='font-family:monospace;color:#0B172A;font-weight:bold;'>" + email + "</span>" +
                "                </div>" +
                "                <div style='font-size:14px;color:#334155;'>" +
                "                  <strong>Mật khẩu khởi tạo:</strong> <span style='font-family:monospace;background:#FEF3C7;padding:3px 8px;border-radius:6px;color:#92400E;font-weight:bold;'>" + password + "</span>" +
                "                </div>" +
                "              </div>" +
                "              <!-- CTA Button -->" +
                "              <div style='text-align:center;margin:30px 0;'>" +
                "                <a href='http://localhost:5173/login' style='background-color:#0B172A;color:#FFFFFF;padding:14px 32px;text-decoration:none;border-radius:10px;font-weight:bold;display:inline-block;letter-spacing:0.5px;box-shadow:0 4px 12px rgba(11,23,42,0.2);'>Đăng Nhập Cổng Portal →</a>" +
                "              </div>" +
                "              <p style='color:#64748B;font-size:13px;line-height:1.5;'>" +
                "                🔒 <em>Lưu ý an ninh:</em> Vì lý do bảo mật, vui lòng đăng nhập và thực hiện đổi mật khẩu cá nhân ngay trong lần truy cập đầu tiên." +
                "              </p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Footer -->" +
                "          <tr>" +
                "            <td style='background-color:#F8FAFC;padding:24px;text-align:center;border-top:1px solid #E2E8F0;color:#94A3B8;font-size:12px;'>" +
                "              © 2026 InsurMatch Platform. All rights reserved.<br>" +
                "              Hệ thống quản trị và phân phối bảo hiểm chuyên nghiệp." +
                "            </td>" +
                "          </tr>" +
                "        </table>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }
}
