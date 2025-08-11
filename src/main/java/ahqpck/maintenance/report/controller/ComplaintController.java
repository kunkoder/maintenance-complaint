package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.dto.*;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.AreaRepository;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.service.AreaService;
import ahqpck.maintenance.report.service.ComplaintService;
import ahqpck.maintenance.report.service.EquipmentService;
import ahqpck.maintenance.report.service.PartService;
import ahqpck.maintenance.report.service.UserService;
import ahqpck.maintenance.report.util.ImportUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserRepository userRepository;
    private final AreaRepository areaRepository;
    private final EquipmentRepository equipmentRepository;
    private final PartRepository partRepository;
    private final EquipmentService equipmentService;
    private final PartService partService;
    private final AreaService areaService;
    private final UserService userService;

    // === LIST COMPLAINTS ===
    @GetMapping
    public String listComplaints(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "reportDate") String sortBy,
            @RequestParam(defaultValue = "false") boolean asc,
            Model model) {

        try {
            int zeroBasedPage = page - 1;
            Page<ComplaintDTO> complaintPage = complaintService.getAllComplaints(keyword, zeroBasedPage, size, sortBy,
                    asc);

            model.addAttribute("complaints", complaintPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

            model.addAttribute("title", "Complaint List");
            model.addAttribute("sortFields", new String[] {
                    "id", "subject", "status", "priority", "category",
                    "reportDate", "updatedAt", "closeTime"
            });

            // Load dropdown data
            model.addAttribute("users", getAllUsersForDropdown());
            model.addAttribute("areas", getAllAreasForDropdown());
            model.addAttribute("equipments", getAllEquipmentsForDropdown());
            // model.addAttribute("parts", getAllPartsForDropdown());

            // Empty DTO for create form
            model.addAttribute("complaintDTO", new ComplaintDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load complaints: " + e.getMessage());
        }

        return "complaint/index";
    }

    @GetMapping("/{id}")
    public String getComplaintDetail(@PathVariable String id, Model model) {
        try {
            ComplaintDTO complaintDTO = complaintService.getComplaintById(id);
            System.out.println("id => " + complaintDTO);
            model.addAttribute("complaint", complaintDTO);
            model.addAttribute("title", "Complaint Detail");

            // Load dropdown data
            model.addAttribute("users", getAllUsersForDropdown());
            model.addAttribute("areas", getAllAreasForDropdown());
            model.addAttribute("equipments", getAllEquipmentsForDropdown());

            return "complaint/detail"; // → src/main/resources/templates/complaint/detail.html

        } catch (NotFoundException e) {
            model.addAttribute("error", "Complaint not found: " + e.getMessage());
            // return "error/404"; // or redirect to list
            return "complaint/detail";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load complaint: " + e.getMessage());
            // return "error/generic";
            return "complaint/detail";
        }
    }

    // === CREATE COMPLAINT ===
    @PostMapping
    public String createComplaint(
            @Valid @ModelAttribute ComplaintDTO complaintDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageBefore", required = false) MultipartFile imageBefore,
            RedirectAttributes ra) {

        System.out.println("dto complaint" + complaintDTO);
        if (bindingResult.hasErrors()) {
            handleBindingErrors(bindingResult, ra, complaintDTO);
            return "redirect:/complaints";
        }

        try {
            complaintService.createComplaint(complaintDTO, imageBefore);
            ra.addFlashAttribute("success", "Complaint created successfully.");
            return "redirect:/complaints";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("complaintDTO", complaintDTO);
            return "redirect:/complaints";
        }
    }

    // === UPDATE COMPLAINT ===
    @PostMapping("/update")
    public String updateComplaint(
            // @RequestParam String partsUsedJson,
            @Valid @ModelAttribute ComplaintDTO complaintDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

                System.out.println(complaintDTO);
        if (bindingResult.hasErrors()) {
            handleBindingErrors(bindingResult, ra, complaintDTO);
            return "redirect:/complaints";
        }

        try {
            // ObjectMapper mapper = new ObjectMapper();
            // List<ComplaintPartDTO> partsUsed = mapper.readValue(
            //         partsUsedJson,
            //         new TypeReference<List<ComplaintPartDTO>>() {
            //         });
            //         System.out.println(partsUsedJson);
            // complaintDTO.setPartsUsed(partsUsed);
            // System.out.println(partsUsedJson);
            complaintService.updateComplaint(complaintDTO);
            ra.addFlashAttribute("success", "Complaint updated successfully.");
            // ra.addFlashAttribute("complaintDTO", complaintDTO);
            return "redirect:/complaints";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("complaintDTO", complaintDTO);
            // return "complaint/detail";
            return "redirect:/complaints";
        }
    }

    // === DELETE COMPLAINT ===
    @GetMapping("/delete/{id}")
    public String deleteComplaint(@PathVariable String id, RedirectAttributes ra) {
        try {
            complaintService.deleteComplaint(id);
            ra.addFlashAttribute("success", "Complaint deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/complaints";
    }

    // @PostMapping("/import")
    // public String importComplaints(
    //         @RequestParam("data") String dataJson,
    //         @RequestParam(value = "sheet", required = false) String sheet,
    //         @RequestParam(value = "headerRow", required = false) Integer headerRow,
    //         RedirectAttributes ra) {

    //     try {
    //         ObjectMapper mapper = new ObjectMapper();
    //         List<Map<String, Object>> data = mapper.readValue(dataJson,
    //                 new TypeReference<List<Map<String, Object>>>() {
    //                 });

    //         ImportUtil.ImportResult result = complaintService.importComplaintsFromExcel(data);

    //         if (result.getImportedCount() > 0 && !result.hasErrors()) {
    //             ra.addFlashAttribute("success",
    //                     "Successfully imported " + result.getImportedCount() + " complaint record(s).");
    //         } else if (result.getImportedCount() > 0) {
    //             StringBuilder msg = new StringBuilder("Imported ").append(result.getImportedCount())
    //                     .append(" record(s), but ").append(result.getErrorMessages().size()).append(" error(s):");
    //             for (String err : result.getErrorMessages()) {
    //                 msg.append("|").append(err);
    //             }
    //             ra.addFlashAttribute("error", msg.toString());
    //         } else {
    //             StringBuilder msg = new StringBuilder("Failed to import any complaint:");
    //             for (String err : result.getErrorMessages()) {
    //                 msg.append("|").append(err);
    //             }
    //             ra.addFlashAttribute("error", msg.toString());
    //         }

    //         return "redirect:/complaints";

    //     } catch (Exception e) {
    //         ra.addFlashAttribute("error", "Bulk import failed: " + e.getMessage());
    //         return "redirect:/complaints";
    //     }
    // }

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

    private List<UserDTO> getAllUsersForDropdown() {
        return userService.getAllUsers(null, 0, Integer.MAX_VALUE, "name", true)
                .getContent().stream()
                .collect(Collectors.toList());
    }

    private List<AreaDTO> getAllAreasForDropdown() {
        return areaService.getAllAreas(null, 0, Integer.MAX_VALUE, "name", true)
                .getContent().stream()
                .collect(Collectors.toList());
    }

    private List<EquipmentDTO> getAllEquipmentsForDropdown() {
        return equipmentService.getAllEquipments(null, 0, Integer.MAX_VALUE, "name", true)
                .getContent().stream()
                .collect(Collectors.toList());
    }
}

// package ahqpck.maintenance.report.controller;

// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;

// @Controller
// public class ComplaintController {

// @GetMapping("/complaints")
// public String index(Model model) {
// model.addAttribute("title", "Complaint List");
// return "complaint/index";
// }

// @GetMapping("/complaints/{id}")
// public String detail(@PathVariable("id") String id, Model model) {
// model.addAttribute("title", "Complaint Detail");
// model.addAttribute("complaintId", id);
// return "complaint/detail";
// }
// }
