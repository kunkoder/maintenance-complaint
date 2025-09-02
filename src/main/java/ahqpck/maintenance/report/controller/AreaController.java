package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.ComplaintDTO;
import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.service.AreaService;
import ahqpck.maintenance.report.service.UserService;
import ahqpck.maintenance.report.util.ImportUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;
    private final UserRepository userRepository; // For populating responsiblePerson dropdown
    private final UserService userService;

    @Value("${app.upload-area-image.dir:src/main/resources/static/upload/area/image}")
    private String uploadDir; // Not used now, but reserved for future

    // === LIST AREAS ===
    @GetMapping
    public String listAreas(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            Model model) {

        try {
            int zeroBasedPage = page - 1;
            Page<AreaDTO> areaPage = areaService.getAllAreas(keyword, zeroBasedPage, size, sortBy, asc);

            model.addAttribute("areas", areaPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

            model.addAttribute("title", "Areas");
            model.addAttribute("sortFields", new String[]{
                    "code", "name", "status", "description"
            });

            // Load users for responsiblePerson dropdown
            // List<UserDTO> users = userRepository.findAll().stream()
            //         .map(this::mapToUserDTO)
            //         .collect(Collectors.toList());


            List<UserDTO> users =  userService.getAllUsers(null, 0, Integer.MAX_VALUE, "name", true)
                .getContent().stream()
                .collect(Collectors.toList());
    
            

            model.addAttribute("users", users);

            // Empty DTO for create form
            model.addAttribute("areaDTO", new AreaDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load areas: " + e.getMessage());
        }

        return "area/index";
    }

    // === CREATE AREA ===
    @PostMapping
    public String createArea(
            @Valid @ModelAttribute AreaDTO areaDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

                System.out.println("Area" + areaDTO);

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("areaDTO", areaDTO);
            return "redirect:/areas";
        }

        try {
            areaService.createArea(areaDTO);
            ra.addFlashAttribute("success", "Area created successfully.");
            return "redirect:/areas";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("areaDTO", areaDTO);
            return "redirect:/areas";
        }
    }

    // === UPDATE AREA ===
    @PostMapping("/update")
    public String updateArea(
            @Valid @ModelAttribute AreaDTO areaDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {
                System.out.println("Area" + areaDTO);

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("areaDTO", areaDTO);
            return "redirect:/areas";
        }

        try {
            areaService.updateArea(areaDTO);
            ra.addFlashAttribute("success", "Area updated successfully.");
            return "redirect:/areas";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("areaDTO", areaDTO);
            return "redirect:/areas";
        }
    }

    // === DELETE AREA ===
    @GetMapping("/delete/{id}")
    public String deleteArea(@PathVariable String id, RedirectAttributes ra) {
        try {
            areaService.deleteArea(id);
            ra.addFlashAttribute("success", "Area deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/areas";
    }

    @PostMapping("/import")
    public String importComplaints(
            @RequestParam("data") String dataJson,
            @RequestParam(value = "sheet", required = false) String sheet,
            @RequestParam(value = "headerRow", required = false) Integer headerRow,
            RedirectAttributes ra) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> data = mapper.readValue(dataJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            // ra.addFlashAttribute("error", data);

            ImportUtil.ImportResult result = areaService.importAreasFromExcel(data);

            if (result.getImportedCount() > 0 && !result.hasErrors()) {
                ra.addFlashAttribute("success",
                        "Successfully imported " + result.getImportedCount() + " area record(s).");
            } else if (result.getImportedCount() > 0) {
                StringBuilder msg = new StringBuilder("Imported ").append(result.getImportedCount())
                        .append(" record(s), but ").append(result.getErrorMessages().size()).append(" error(s):");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            } else {
                StringBuilder msg = new StringBuilder("Failed to import any area:");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            }

            return "redirect:/areas";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Bulk import failed: " + e.getMessage());
            return "redirect:/areas";
        }
    }

    // === HELPERS ===

    private void handleBindingErrors(BindingResult bindingResult, RedirectAttributes ra, ComplaintDTO dto) {
        String errorMessage = bindingResult.getAllErrors().stream()
                .map(error -> {
                    String field = (error instanceof org.springframework.validation.FieldError)
                            ? ((org.springframework.validation.FieldError) error).getField()
                            : "Input";
                    String message = error.getDefaultMessage();
                    return field + ": " + message;
                })
                .collect(Collectors.joining(" | "));

        ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
        ra.addFlashAttribute("complaintDTO", dto);
    }

    // Map User → UserDTO for dropdown
    private UserDTO mapToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        dto.setStatus(user.getStatus());
        return dto;
    }
}