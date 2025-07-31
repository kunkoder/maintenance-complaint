package ahqpck.maintenance.report.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PartController {

    @GetMapping("/parts")
    public String index(Model model) {
        model.addAttribute("title", "Parts Inventory");
        return "part/index";
    }
}
