package ahqpck.maintenance.report.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ComplaintController {

    @GetMapping("/complaint")
    public String index(Model model) {
        // model.addAttribute("title", "Dashboard");
        return "complaint/index";
    }
}
