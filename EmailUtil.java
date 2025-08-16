package ahqpck.maintenance.report.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import ahqpck.maintenance.report.constant.MessageConstant;
import ahqpck.maintenance.report.constant.UrlListConstant;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Component
public class EmailUtil {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${client.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String sender;

    public void sendVerificationLink(String email, String subject, String description, String token) {
        
        String verificationLink = UrlListConstant.VERIFICATION_URL.formatted(
            baseUrl, description.replace(" ", "-"),
            email, token);
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom(new InternetAddress(sender));
            message.setRecipients(MimeMessage.RecipientType.TO, email);
            message.setSubject(subject);

            String htmlContent = MessageConstant.EMAIL_VERIFICATION_BODY
                .replace("{base-url}", baseUrl)
                .replace("{description}", description)
                .replace("{email}", email)
                .replace("{verification-link}", verificationLink);
            message.setContent(htmlContent, "text/html; charset=utf-8");
            javaMailSender.send(message);
        }
        catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
