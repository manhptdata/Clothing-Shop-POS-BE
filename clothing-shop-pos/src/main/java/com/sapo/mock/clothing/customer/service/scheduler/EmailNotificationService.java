package com.sapo.mock.clothing.customer.service.scheduler;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Autowired
    private JavaMailSender mailSender;

    public void sendBirthdayVoucherEmail(String customerEmail, String customerName, String voucherCode) {
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            log.warn("Khách hàng {} không có Email, bỏ qua gửi thư.", customerName);
            return;
        }

        try {
            log.info("============== BẮT ĐẦU GỬI EMAIL ==============");
            log.info("Đang gửi Email chúc mừng sinh nhật đến: {}", customerEmail);

            MimeMessage message = mailSender.createMimeMessage();
            // Bật true để cho phép gửi giao diện HTML
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(customerEmail);
            helper.setSubject("🎉 Chúc Mừng Sinh Nhật - Quà Tặng Từ Sapo POS");

            // Thiết kế giao diện HTML cho thư đính kèm mã Voucher
            String htmlMsg = "<div style='font-family: Arial, sans-serif; text-align: center; padding: 20px; border: 1px solid #ddd; border-radius: 10px; max-width: 500px; margin: auto;'>"
                    + "<h2 style='color: #007bff;'>Chúc Mừng Sinh Nhật! 🎂</h2>"
                    + "<p>Chào <b>" + customerName + "</b>,</p>"
                    + "<p>Sapo POS xin gửi đến bạn lời chúc sinh nhật vui vẻ, hạnh phúc và thành công.</p>"
                    + "<p>Thay cho món quà nhỏ, Sapo POS gửi tặng bạn một Voucher mua sắm đặc biệt:</p>"
                    + "<div style='background-color: #f8f9fa; padding: 15px; font-size: 24px; font-weight: bold; color: #dc3545; border: 2px dashed #dc3545; display: inline-block; margin: 20px 0;'>"
                    + voucherCode
                    + "</div>"
                    + "<p><i>(Vui lòng đưa mã này cho nhân viên thu ngân khi thanh toán)</i></p>"
                    + "<hr style='border-top: 1px solid #eee; margin-top: 30px;' />"
                    + "<p style='font-size: 12px; color: #888;'>Cảm ơn bạn đã luôn đồng hành cùng hệ thống phần mềm Sapo POS.</p>"
                    + "</div>";

            helper.setText(htmlMsg, true);
            mailSender.send(message);

            log.info("✅ THÀNH CÔNG: Đã gửi thư chứa Voucher [{}] tới hòm thư của khách hàng {}!", voucherCode, customerName);
            log.info("===============================================");

        } catch (MessagingException e) {
            log.error("Lỗi khi gửi Email sinh nhật: ", e);
        }
    }
}
