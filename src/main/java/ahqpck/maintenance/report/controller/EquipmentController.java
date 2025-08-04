// src/main/java/ahqpck/maintenance/report/controller/EquipmentController.java
package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.service.EquipmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @Value("${app.upload-equipment-image.dir:src/main/resources/static/upload/equipment/image}")
    private String uploadDir;

    @GetMapping
    public String listEquipments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            Model model) {

        try {
            var equipmentPage = equipmentService.getAllEquipments(keyword, page, size, sortBy, asc);
            model.addAttribute("equipments", equipmentPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

            model.addAttribute("title", "Equipments");
            model.addAttribute("sortFields", new String[]{
                "code", "name", "model", "manufacturer", "serialNo", "qty", "capacity", "manufacturedDate", "commissionedDate"
            });
            model.addAttribute("equipmentDTO", new EquipmentDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load equipment: " + e.getMessage());
        }

        return "equipment/index";
    }

    @PostMapping
    public String createEquipment(
            @Valid @ModelAttribute EquipmentDTO equipmentDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("equipmentDTO", equipmentDTO);
            return "redirect:/equipments";
        }

        try {
            equipmentService.createEquipment(equipmentDTO, imageFile);
            ra.addFlashAttribute("success", "Equipment created successfully.");
            return "redirect:/equipments";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("equipmentDTO", equipmentDTO);
            return "redirect:/equipments";
        }
    }

    @PostMapping("/update")
    public String updateEquipment(
            @Valid @ModelAttribute EquipmentDTO equipmentDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "deleteImage", required = false, defaultValue = "false") boolean deleteImage,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("equipmentDTO", equipmentDTO);
            return "redirect:/equipments";
        }

        try {
            equipmentService.updateEquipment(equipmentDTO, imageFile, deleteImage);
            ra.addFlashAttribute("success", "Equipment updated successfully.");
            return "redirect:/equipments";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("equipmentDTO", equipmentDTO);
            return "redirect:/equipments";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteEquipment(@PathVariable String id, RedirectAttributes ra) {
        try {
            equipmentService.deleteEquipment(id);
            ra.addFlashAttribute("success", "Equipment deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/equipments";
    }
}