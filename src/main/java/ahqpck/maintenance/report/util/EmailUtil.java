package ahqpck.maintenance.report.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import ahqpck.maintenance.report.constant.MessageConstant;
import ahqpck.maintenance.report.constant.UrlListConstant;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailUtil {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine; // Thymeleaf template engine

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Send account activation email
     */
    public void sendAccountActivationEmail(String email, String token) {
        String subject = "Activate Your Account";
        String verificationLink = baseUrl + "/activate-account?email=" + email + "&token=" + token;

        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("verificationLink", verificationLink);

        String htmlContent = templateEngine.process("email/account-activation", context);
        sendEmail(email, subject, htmlContent);
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email, String token) {
        String subject = "Reset Your Password";
        String verificationLink = baseUrl + "/reset-password?email=" + email + "&token=" + token;

        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("verificationLink", verificationLink);

        String htmlContent = templateEngine.process("email/password-reset", context);
        sendEmail(email, subject, htmlContent);
    }

    /**
     * Reusable method to send email
     */
    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(sender);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to: " + to, e);
        }
    }
}