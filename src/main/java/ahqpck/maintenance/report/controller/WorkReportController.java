package ahqpck.maintenance.report.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WorkReportController {

    @GetMapping("/work-reports")
    public String index(Model model) {
        model.addAttribute("title", "Work Report");
        return "work-report/index";
    }
}
