package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.dto.RoleDTO;
import ahqpck.maintenance.report.entity.Role;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.RoleRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.specification.UserSpecification;
import ahqpck.maintenance.report.util.FileUploadUtil;
import ahqpck.maintenance.report.util.ImportUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${app.upload-user-avatar.dir:src/main/resources/static/upload/user/avatar}")
    private String uploadDir;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // Add this repository
    private final Validator validator;
    private final FileUploadUtil fileUploadUtil;

    public Page<UserDTO> getAllUsers(String keyword, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<User> spec = UserSpecification.search(keyword);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        return userPage.map(this::toDTO);
    }

    public UserDTO getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));
        return toDTO(user);
    }

    public void createUser(UserDTO dto, MultipartFile imageFile) {
        if (userRepository.existsByEmployeeIdIgnoringCase(dto.getEmployeeId())) {
            throw new IllegalArgumentException("User with this employee ID already exists.");
        }

        if (userRepository.existsByEmailIgnoringCase(dto.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        User user = new User();
        mapToEntity(user, dto);

        // Handle image upload
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = fileUploadUtil.saveFile(uploadDir, imageFile, "image");
                user.setAvatar(fileName);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        // Handle roles
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            Set<Role> roles = dto.getRoles().stream()
                    .map(roleDTO -> roleRepository.findByName(Role.Name.valueOf(roleDTO.getName().toString()))
                            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleDTO.getName())))
                    .collect(Collectors.toSet());
            user.getRoles().addAll(roles);
        }

        userRepository.save(user);
    }

    // ----------------- Mapping Methods -----------------

    private void mapToEntity(User user, UserDTO dto) {
        user.setName(dto.getName().trim());
        user.setEmployeeId(dto.getEmployeeId().trim());
        user.setEmail(dto.getEmail().trim());
        // Optional: hash password if included in DTO
        // if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
        //     user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        // }
    
        // ✅ Handle Status: default to INACTIVE if not provided
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : User.Status.INACTIVE);
    
        // ✅ Handle Roles: if none provided, assign default role: VIEWER
        Set<Role> roles;
        if (dto.getRoles() == null || dto.getRoles().isEmpty()) {
            Role viewerRole = roleRepository.findByName(Role.Name.VIEWER)
                .orElseThrow(() -> new IllegalStateException("Default role VIEWER not found in database. Please seed roles."));
            roles = Set.of(viewerRole);
        } else {
            roles = dto.getRoles().stream()
                .map(roleDTO -> {
                    try {
                        Role.Name roleName = Role.Name.valueOf(roleDTO.getName().toString().toUpperCase());
                        return roleRepository.findByName(roleName)
                            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid role name: " + roleDTO.getName());
                    }
                })
                .collect(Collectors.toSet());
        }
    
        user.getRoles().clear();
        user.getRoles().addAll(roles);
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setStatus(user.getStatus()); // e.g., ACTIVE, INACTIVE

        // ✅ Map roles
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            dto.setRoles(user.getRoles().stream()
                .map(RoleDTO::new)
                .collect(Collectors.toSet()));
        } else {
            // Should not happen due to business logic, but safe fallback
            dto.setRoles(Collections.emptySet());
        }

        return dto;
    }

}