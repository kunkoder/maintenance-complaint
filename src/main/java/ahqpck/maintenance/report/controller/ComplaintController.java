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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
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
            Page<ComplaintDTO> complaintPage = complaintService.getAllComplaints(keyword, zeroBasedPage, size, sortBy, asc);

            model.addAttribute("complaints", complaintPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

            model.addAttribute("title", "Complaint List");
            model.addAttribute("sortFields", new String[]{
                    "id", "subject", "status", "priority", "category",
                    "reportDate", "updatedAt", "closeTime"
            });

            // Load dropdown data
            model.addAttribute("users", getAllUsersForDropdown());
            model.addAttribute("areas", getAllAreasForDropdown());
            model.addAttribute("equipments", getAllEquipmentsForDropdown());
            model.addAttribute("parts", getAllPartsForDropdown());

            // Empty DTO for create form
            model.addAttribute("complaintDTO", new ComplaintDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load complaints: " + e.getMessage());
        }

        return "complaint/index";
    }

    // === CREATE COMPLAINT ===
    @PostMapping
    public String createComplaint(
            @Valid @ModelAttribute ComplaintDTO complaintDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            handleBindingErrors(bindingResult, ra, complaintDTO);
            return "redirect:/complaints";
        }

        try {
            complaintService.createComplaint(complaintDTO);
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
            @Valid @ModelAttribute ComplaintDTO complaintDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            handleBindingErrors(bindingResult, ra, complaintDTO);
            return "redirect:/complaints";
        }

        try {
            complaintService.updateComplaint(complaintDTO);
            ra.addFlashAttribute("success", "Complaint updated successfully.");
            return "redirect:/complaints";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("complaintDTO", complaintDTO);
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

    // === HELPERS ===

    private void handleBindingErrors(BindingResult bindingResult, RedirectAttributes ra, ComplaintDTO dto) {
        String errorMessage = bindingResult.getAllErrors().stream()
                .map(error -> {
                    String field = (error instanceof org.springframework.validation.FieldError) ?
                            ((org.springframework.validation.FieldError) error).getField() : "Input";
                    String message = error.getDefaultMessage();
                    return field + ": " + message;
                })
                .collect(Collectors.joining(" | "));

        ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
        ra.addFlashAttribute("complaintDTO", dto);
    }

    // private List<UserDTO> getAllUsersForDropdown() {
    //     return userService.getAllUsers(null, 0, Integer.MAX_VALUE, "name", true)
    //             .getContent().stream()
    //             .map(this::mapToUserDTO)
    //             .collect(Collectors.toList());
    // }

    private List<UserDTO> getAllUsersForDropdown() {
        return userRepository.findAll().stream()
        .map(this::mapToUserDTO)
        .collect(Collectors.toList());
    }


    private List<AreaDTO> getAllAreasForDropdown() {
        return areaService.getAllAreas(null, 0, Integer.MAX_VALUE, "name", true)
                .getContent().stream()
                .collect(Collectors.toList());
    }

    // private List<AreaDTO> getAllAreasForDropdown() {
    //     return areaRepository.findAll().stream()
    //     .map(this::mapToAreaDTO)
    //     .collect(Collectors.toList());
    // }

    private List<EquipmentDTO> getAllEquipmentsForDropdown() {
        return equipmentService.getAllEquipments(null, 0, Integer.MAX_VALUE, "name", true)
                .getContent().stream()
                .collect(Collectors.toList());
    }

    // private List<EquipmentDTO> getAllEquipmentsForDropdown() {
    //     return equipmentRepository.findAll().stream()
    //     .map(this::mapToEquipmentDTO)
    //     .collect(Collectors.toList());
    // }

    private List<PartDTO> getAllPartsForDropdown() {
        return partService.getAllParts(null, 0, Integer.MAX_VALUE, "name", true)
                .getContent().stream()
                .collect(Collectors.toList());
    }

    // Map User → UserDTO
    private UserDTO mapToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        dto.setStatus(user.getStatus());
        return dto;
    }

    // Map Area → AreaDTO (minimal)
    private AreaDTO mapToAreaDTO(Area area) {
        AreaDTO dto = new AreaDTO();
        dto.setId(area.getId());
        dto.setCode(area.getCode());
        dto.setName(area.getName());
        dto.setStatus(area.getStatus());
        return dto;
    }

    private EquipmentDTO mapToEquipmentDTO(Equipment equipment) {
        EquipmentDTO dto = new EquipmentDTO();
        dto.setId(equipment.getId());
        dto.setCode(equipment.getCode());
        dto.setName(equipment.getName());
        return dto;
    }
}












// package ahqpck.maintenance.report.controller;

// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;

// @Controller
// public class ComplaintController {

//     @GetMapping("/complaints")
//     public String index(Model model) {
//         model.addAttribute("title", "Complaint List");
//         return "complaint/index";
//     }
    
//     @GetMapping("/complaints/{id}")
//     public String detail(@PathVariable("id") String id, Model model) {
//         model.addAttribute("title", "Complaint Detail");
//         model.addAttribute("complaintId", id);
//         return "complaint/detail";
//     }
// }
