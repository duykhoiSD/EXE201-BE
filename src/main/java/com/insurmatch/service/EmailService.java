package com.insurmatch.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@insurmatch.us}")
    private String fromEmail;

    /**
     * Gửi email mã OTP xác thực tài khoản.
     * Tự động fallback in ra Console Log nếu chưa cấu hình Mail Server thật.
     */
    public boolean sendOtpEmail(String toEmail, String otp, String recipientName) {
        logOtpToConsole(toEmail, otp, recipientName);

        if (mailSender == null) {
            log.info("JavaMailSender is not configured. OTP printed to console.");
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "InsurMatch CRM");
            helper.setTo(toEmail);
            helper.setSubject("🛡️ [InsurMatch] Mã xác thực OTP đăng ký tài khoản: " + otp);

            String htmlContent = buildOtpHtmlContent(recipientName, otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Successfully sent OTP email to {}", toEmail);
            return true;
        } catch (Exception e) {
            log.warn("Could not send real email to {} (Reason: {}). OTP is available in console.", toEmail, e.getMessage());
            return true;
        }
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

        if (mailSender == null) {
            log.info("JavaMailSender is not configured. Reset OTP printed to console.");
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "InsurMatch Security");
            helper.setTo(toEmail);
            helper.setSubject("🔑 [InsurMatch] Yêu cầu đặt lại mật khẩu: " + otp);

            String htmlContent = buildResetPasswordHtmlContent(recipientName, otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Successfully sent Password Reset OTP email to {}", toEmail);
            return true;
        } catch (Exception e) {
            log.warn("Could not send real reset email to {} (Reason: {}). OTP is available in console.", toEmail, e.getMessage());
            return true;
        }
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
}
