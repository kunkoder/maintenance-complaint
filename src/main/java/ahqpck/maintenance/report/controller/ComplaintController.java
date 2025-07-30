package ahqpck.maintenance.report.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ComplaintController {

    @GetMapping("/complaints")
    public String index(Model model) {
        model.addAttribute("title", "Complaint List");
        return "complaint/index";
    }
    @GetMapping("/complaints/{id}")
    public String detail(@PathVariable("id") String id, Model model) {
        model.addAttribute("title", "Complaint Detail");
        model.addAttribute("complaintId", id);
        return "complaint/detail";
    }
}
