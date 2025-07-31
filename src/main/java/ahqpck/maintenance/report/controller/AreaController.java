package ahqpck.maintenance.report.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AreaController {

    @GetMapping("/areas")
    public String index(Model model) {
        model.addAttribute("title", "Areas");
        return "area/index";
    }
}
