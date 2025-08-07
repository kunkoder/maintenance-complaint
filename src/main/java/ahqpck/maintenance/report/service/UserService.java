package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.exception.NotFoundException;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${app.upload-user-image.dir:src/main/resources/static/upload/user/image}")
    private String uploadDir;

    private final UserRepository userRepository;
    private final Validator validator;

    private final FileUploadUtil fileUploadUtil;
    private final ImportUtil importUtil;

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
            throw new IllegalArgumentException("User with this employee id already exists.");
        }

        User user = new User();
        mapToEntity(user, dto);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = fileUploadUtil.saveFile(uploadDir, imageFile, "image");
                user.setAvatar(fileName);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        userRepository.save(user);
    }

    private void mapToEntity(User user, UserDTO dto) {
        user.setCode(dto.getCode().trim());
        user.setName(dto.getName().trim());
        user.setModel(dto.getModel());
        user.setUnit(dto.getUnit());
        user.setQty(dto.getQty() != null ? dto.getQty() : 0);
        user.setManufacturer(dto.getManufacturer());
        user.setSerialNo(dto.getSerialNo());
        user.setManufacturedDate(dto.getManufacturedDate());
        user.setCommissionedDate(dto.getCommissionedDate());
        user.setCapacity(dto.getCapacity());
        user.setRemarks(dto.getRemarks());
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setCode(user.getCode());
        dto.setName(user.getName());
        dto.setModel(user.getModel());
        dto.setUnit(user.getUnit());
        dto.setQty(user.getQty());
        dto.setManufacturer(user.getManufacturer());
        dto.setSerialNo(user.getSerialNo());
        dto.setManufacturedDate(user.getManufacturedDate());
        dto.setCommissionedDate(user.getCommissionedDate());
        dto.setCapacity(user.getCapacity());
        dto.setRemarks(user.getRemarks());
        dto.setImage(user.getImage());
        return dto;
    }
}