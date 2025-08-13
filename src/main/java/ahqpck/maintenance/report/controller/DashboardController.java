package ahqpck.maintenance.report.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import ahqpck.maintenance.report.dto.DashboardDTO;
import ahqpck.maintenance.report.service.DashboardService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public String dashboard(Model model) {
    //     DashboardDTO data = dashboardService.getDashboardData();
    // // Even if service returns null, make sure we pass a non-null object
    // model.addAttribute("dashboard", data != null ? data : new DashboardDTO());
        model.addAttribute("title", "Overview");
        return "dashboard/overview";
    }
}
