package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.dto.RoleDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.Role;
import ahqpck.maintenance.report.exception.ImportException;
import ahqpck.maintenance.report.exception.ValidationException;
import ahqpck.maintenance.report.repository.RoleRepository;
import ahqpck.maintenance.report.service.UserService;
import ahqpck.maintenance.report.util.ImportUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository; // To populate roles in model

    @Value("${app.upload-user-image.dir:src/main/resources/static/upload/user/image}")
    private String uploadDir;

    // === LIST USERS ===
    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            Model model) {

        try {
            int zeroBasedPage = page - 1;
            var userPage = userService.getAllUsers(keyword, zeroBasedPage, size, sortBy, asc);

            model.addAttribute("users", userPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

            model.addAttribute("title", "User Management");
            model.addAttribute("sortFields", new String[] {
                    "name", "employeeId", "email", "status", "createdAt", "activatedAt"
            });

            // Provide available roles for UI (e.g., dropdown in form)
            List<RoleDTO> roles = roleRepository.findAll().stream()
                    .map(RoleDTO::new)
                    .collect(Collectors.toList());
            model.addAttribute("roles", roles);

            // Empty DTO for create form
            model.addAttribute("userDTO", new UserDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load users: " + e.getMessage());
        }

        return "user/index";
    }

    // === CREATE USER ===
    @PostMapping
    public String createUser(
            @Valid @ModelAttribute UserDTO userDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra) {

        if (userDTO.getRoleNames() != null && !userDTO.getRoleNames().isEmpty()) {
            Set<RoleDTO> roleDTOs = userDTO.getRoleNames().stream()
                    .map(name -> {
                        Role.Name roleName = Role.Name.valueOf(name);
                        RoleDTO dto = new RoleDTO();
                        dto.setName(roleName);
                        return dto;
                    })
                    .collect(Collectors.toSet());
            userDTO.setRoles(roleDTOs);
        }

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("userDTO", userDTO);
            return "redirect:/users";
        }

        try {
            userService.createUser(userDTO, imageFile);
            ra.addFlashAttribute("success", "User created successfully.");
            return "redirect:/users";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("userDTO", userDTO);
            return "redirect:/users";
        }
    }

    // === UPDATE USER ===
    @PostMapping("/update")
    public String updateUser(
            @Valid @ModelAttribute UserDTO userDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "deleteImage", required = false, defaultValue = "false") boolean deleteImage,
            RedirectAttributes ra) {

        if (userDTO.getRoleNames() != null && !userDTO.getRoleNames().isEmpty()) {
            Set<RoleDTO> roleDTOs = userDTO.getRoleNames().stream()
                    .map(name -> {
                        Role.Name roleName = Role.Name.valueOf(name);
                        RoleDTO dto = new RoleDTO();
                        dto.setName(roleName);
                        return dto;
                    })
                    .collect(Collectors.toSet());
            userDTO.setRoles(roleDTOs);
        }

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("userDTO", userDTO);
            return "redirect:/users";
        }

        try {
            userService.updateUser(userDTO, imageFile, deleteImage);
            ra.addFlashAttribute("success", "User updated successfully.");
            return "redirect:/users";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("userDTO", userDTO);
            return "redirect:/users";
        }
    }

    // === DELETE USER ===
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable String id, RedirectAttributes ra) {
        try {
            userService.deleteUser(id);
            ra.addFlashAttribute("success", "User deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }
}