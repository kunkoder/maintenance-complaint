package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.ResetPasswordDTO;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailUtil emailUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void activateAccount(String email, String token) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!token.equals(user.getAccountActivationToken())) {
            throw new IllegalArgumentException("Invalid activation token");
        }

        user.setStatus(User.Status.ACTIVE);
        user.setActivatedAt(LocalDateTime.now());
        user.setAccountActivationToken(null);
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        // userRepository.save(user);

        try {
            emailUtil.sendPasswordResetEmail(email, token);
            userRepository.save(user);
        } catch (Exception e) {
            // Optional: delete user if email fails?
            // Or mark as "email_failed" and retry later
            throw new RuntimeException("Failed to send verification email to " + user.getEmail(), e);
        }
    }

    @Transactional
    public void verifyResetPassword(String email, String token) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!token.equals(user.getPasswordResetToken())) {
            throw new IllegalArgumentException("Invalid password reset token");
        }

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token has expired");
        }

        // user.setPasswordResetToken(null);
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {
        String email = dto.getEmail();
        String token = dto.getToken();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!token.equals(user.getPasswordResetToken())) {
            throw new IllegalArgumentException("Invalid password reset token");
        }

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }
}