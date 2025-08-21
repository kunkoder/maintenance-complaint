package ahqpck.maintenance.report.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class DashboardController {

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("title", "Overview");
        return "dashboard/overview";
    }
}
