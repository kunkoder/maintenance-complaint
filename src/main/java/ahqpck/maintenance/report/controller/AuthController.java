package ahqpck.maintenance.report.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("title", "Register");
        return "area/index";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("title", "Login");
        return "area/index";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        model.addAttribute("title", "Areas");
        return "area/index";
    }

    @GetMapping("/reset-password")
    public String resetPassword(Model model) {
        model.addAttribute("title", "Areas");
        return "area/index";
    }
}
