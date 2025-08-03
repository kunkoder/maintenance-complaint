package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.dto.PartDTO;
import ahqpck.maintenance.report.service.PartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/parts")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;

    @Value("${app.upload.dir:uploads/parts}")
    private String uploadDir;

    @GetMapping
    public String listParts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            Model model) {

        try {
            var partsPage = partService.getAllParts(keyword, page, size, sortBy, asc);
            model.addAttribute("parts", partsPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

            model.addAttribute("title", "Parts Inventory");
            model.addAttribute("sortFields", new String[]{"code", "name", "category", "supplier", "stockQuantity"});
            model.addAttribute("partDTO", new PartDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load parts: " + e.getMessage());
        }

        return "part/index";
    }

    @PostMapping
    public String createPart(
            @Valid
            @ModelAttribute PartDTO partDTO,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra) {

        try {
            partService.createPart(partDTO, imageFile);
            ra.addFlashAttribute("success", "Part created successfully.");
            return "redirect:/parts";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/parts";
        }
    }

    // === UPDATE ===
    @PostMapping("/{id}")
    public String updatePart(
            @PathVariable String id,
            @ModelAttribute PartDTO partDTO,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "deleteImage", required = false, defaultValue = "false") boolean deleteImage,
            RedirectAttributes ra) {

        try {
            partService.updatePart(id, partDTO, imageFile, deleteImage);
            ra.addFlashAttribute("success", "Part updated successfully.");
            return "redirect:/parts";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/parts/edit/" + id;
        }
    }

    // === DELETE ===
    @PostMapping("/delete/{id}")
    public String deletePart(@PathVariable String id, RedirectAttributes ra) {
        try {
            partService.deletePart(id);
            ra.addFlashAttribute("success", "Part deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/parts";
    }
}
