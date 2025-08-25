// File: AuthController.java
package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.dto.ForgotPasswordDTO;
import ahqpck.maintenance.report.dto.RegisterDTO;
import ahqpck.maintenance.report.dto.ResetPasswordDTO;
import ahqpck.maintenance.report.dto.RoleDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.Role;
import ahqpck.maintenance.report.service.AuthService;
import ahqpck.maintenance.report.service.UserService;
import ahqpck.maintenance.report.util.WebUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor // Injects final fields (AuthService)
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/login")
    public String showLogin(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "inactive", required = false) String inactive,
            @RequestParam(value = "logout", required = false) String logout,
            RedirectAttributes ra) {

        // Handle inactive account
        if (inactive != null && !ra.getFlashAttributes().containsKey("error")) {
            ra.addFlashAttribute("error",
                    "Your account is not active. Please check your email or contact the administrator to activate it.");
            return "redirect:/login";
        }

        // Convert ?error to flash attribute
        if (error != null && !ra.getFlashAttributes().containsKey("error")) {
            ra.addFlashAttribute("error", "Invalid credentials. Please check your email, ID, or password.");
            return "redirect:/login";
        }

        // Convert ?logout to flash attribute
        if (logout != null && !ra.getFlashAttributes().containsKey("success")) {
            ra.addFlashAttribute("success", "You have been logged out successfully.");
            return "redirect:/login";
        }
        return "auth/login";
    }

    // Note: POST /login is handled by Spring Security
    // No custom login logic needed unless extending authentication

    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("title", "Register");
        model.addAttribute("registerDTO", new RegisterDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String submitRegister(
            @Valid @ModelAttribute RegisterDTO registerDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("registerDTO", registerDTO);
            return "redirect:/register";
        }

        try {
            // Map RegisterDTO to UserDTO
            UserDTO userDTO = new UserDTO();
            userDTO.setName(registerDTO.getName());
            userDTO.setEmail(registerDTO.getEmail());
            userDTO.setEmployeeId(registerDTO.getEmployeeId());
            userDTO.setPassword(registerDTO.getPassword());

            // ✅ Set default role: VIEWER
            RoleDTO viewerRole = new RoleDTO();
            viewerRole.setName(Role.Name.VIEWER);

            userDTO.setRoles(Set.of(viewerRole));

            // Use existing service (DRY!)
            userService.createUser(userDTO, null);

            ra.addFlashAttribute("success", "Registration successful! Please check your email to activate your account before logging in.");
            return "redirect:/login";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("registerDTO", registerDTO);
            return "redirect:/register";
        }
    }

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
            @RequestParam String email,
            @RequestParam String token,
            Model model,
            RedirectAttributes ra) {

        try {
            authService.verifyResetPassword(email, token);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Verification failed: " + e.getMessage());
        }

        model.addAttribute("title", "Reset Password");
        model.addAttribute("resetPassword", new ResetPasswordDTO());
        model.addAttribute("email", email);
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
            return "redirect:/reset-password?email=" + dto.getEmail() + "&token=" + dto.getToken();
        }
    }

    @GetMapping("/activate-account")
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