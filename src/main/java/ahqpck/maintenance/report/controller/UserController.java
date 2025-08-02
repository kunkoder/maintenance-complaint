package ahqpck.maintenance.report.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class UserController {

    @GetMapping("/users")
    public String index(Model model) {
        model.addAttribute("title", "User Management");
        return "user/index";
    }

    @GetMapping("/users/{id}")
    public String detail(@PathVariable("id") String id, Model model) {
        model.addAttribute("title", "My Profile");
        model.addAttribute("userId", id);
        return "user/profile";
    }
}
