// File: AuthController.java
package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.dto.ForgotPasswordDTO;
import ahqpck.maintenance.report.dto.LoginDTO;
import ahqpck.maintenance.report.dto.ResetPasswordDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.service.AuthService;
import ahqpck.maintenance.report.util.WebUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor  // Injects final fields (AuthService)
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String showLogin(Model model) {
        model.addAttribute("title", "Login");
        model.addAttribute("loginDTO", new LoginDTO());
        return "auth/login";
    }

    // Note: POST /login is handled by Spring Security
    // No custom login logic needed unless extending authentication

    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("title", "Register");
        model.addAttribute("registerDTO", new UserDTO());
        return "auth/register";
    }

    // Register POST not implemented (as requested)

    @GetMapping("/forgot-password")
    public String showForgotPassword(Model model) {
        model.addAttribute("title", "Forgot Password");
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordDTO());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String submitForgotPassword(
            @Valid @ModelAttribute ForgotPasswordDTO dto,
            BindingResult bindingResult,
            RedirectAttributes ra) {

        // Reusable validation
        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
            return "redirect:/forgot-password";
        }

        try {
            authService.forgotPassword(dto.getEmail());
            ra.addFlashAttribute("success", "Password reset link has been sent to " + dto.getEmail());
            return "redirect:/login";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    // ==================== RESET PASSWORD ====================

    @GetMapping("/reset-password")
    public String showResetPassword(
            @RequestParam(required = false) String token,
            Model model,
            RedirectAttributes ra) {

        if (token == null || token.trim().isEmpty()) {
            ra.addFlashAttribute("error", "Missing reset token");
            return "redirect:/forgot-password";
        }

        model.addAttribute("title", "Reset Password");
        model.addAttribute("resetPassword", new ResetPasswordDTO());
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String submitResetPassword(
            @Valid @ModelAttribute ResetPasswordDTO dto,
            BindingResult bindingResult,
            RedirectAttributes ra) {

        // Reusable validation
        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
            return "redirect:/reset-password?token=" + dto.getToken();
        }

        try {
            authService.resetPassword(dto);
            ra.addFlashAttribute("success", "Password has been reset successfully. You can now log in.");
            return "redirect:/login";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/reset-password?token=" + dto.getToken();
        }
    }

    @GetMapping("/auth/activate")
    public String activateAccount(
            @RequestParam String email,
            @RequestParam String token,
            RedirectAttributes ra) {

        try {
            authService.activateAccount(email, token);
            ra.addFlashAttribute("success", "Your account has been activated! You can now log in.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Activation failed: " + e.getMessage());
        }

        return "redirect:/login";
    }
}