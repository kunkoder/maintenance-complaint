package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.ComplaintDTO;
import ahqpck.maintenance.report.dto.RoleDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.AreaRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.specification.AreaSpecification;
import ahqpck.maintenance.report.util.ImportUtil;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;
    private final UserRepository userRepository;
    private final Validator validator;
    private final ImportUtil importUtil;

    public Page<AreaDTO> getAllAreas(String keyword, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Area> spec = AreaSpecification.search(keyword);
        Page<Area> areaPage = areaRepository.findAll(spec, pageable);

        return areaPage.map(this::toDTO);
    }

    public AreaDTO getAreaById(String id) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Area not found with ID: " + id));
        return toDTO(area);
    }

    public void createArea(AreaDTO dto) {
        if (areaRepository.existsByCodeIgnoreCase(dto.getCode())) {
            throw new IllegalArgumentException("Area with this code already exists.");
        }

        Area area = new Area();
        mapToEntity(area, dto);
        areaRepository.save(area);
    }

    @Transactional
    public ImportUtil.ImportResult importAreasFromExcel(List<Map<String, Object>> data) {
        List<String> errorMessages = new ArrayList<>();
        int importedCount = 0;

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data to import.");
        }

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                AreaDTO dto = new AreaDTO();

                // ✅ CODE (required)
                String code = importUtil.toString(row.get("code"));
                if (code == null || code.trim().isEmpty()) {
                    throw new IllegalArgumentException("Area code is required");
                }
                dto.setCode(code.trim());

                // ✅ NAME (required)
                String name = importUtil.toString(row.get("name"));
                if (name == null || name.trim().isEmpty()) {
                    throw new IllegalArgumentException("Area name is required");
                }
                dto.setName(name.trim());

                // 🟡 DESCRIPTION (optional)
                dto.setDescription(importUtil.toString(row.get("description")));

                // 🟡 STATUS (optional, default: ACTIVE)
                String statusStr = importUtil.toString(row.get("status"));
                if (statusStr != null && !statusStr.trim().isEmpty()) {
                    try {
                        dto.setStatus(Area.Status.valueOf(statusStr.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid Status value: '" + statusStr + "'");
                    }
                } else {
                    dto.setStatus(Area.Status.ACTIVE); // default
                }

                // ✅ RESPONSIBLE PERSON (required)
                String empIdRaw = importUtil.toString(row.get("responsiblePerson"));
                if (empIdRaw == null || empIdRaw.trim().isEmpty()) {
                    throw new IllegalArgumentException("Responsible person (employee ID) is required");
                }
                String empId = empIdRaw.trim(); // ✅ Assigned once

                User user = userRepository.findByEmployeeId4Roles(empId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "User not found with employee ID: " + empId));

                UserDTO userDTO = new UserDTO();
                userDTO.setId(user.getId());
                userDTO.setEmployeeId(user.getEmployeeId());
                userDTO.setName(user.getName());
                userDTO.setEmail(user.getEmail());
                dto.setResponsiblePerson(userDTO);

                // Final validation (optional fields like description are allowed to be null)
                Set<ConstraintViolation<AreaDTO>> violations = validator.validate(dto);
                List<String> filteredMessages = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.toList());

                if (!filteredMessages.isEmpty()) {
                    throw new IllegalArgumentException("Validation failed: " + String.join(", ", filteredMessages));
                }

                // Check duplicate code
                if (areaRepository.existsByCodeIgnoreCase(dto.getCode())) {
                    throw new IllegalArgumentException("Area with code '" + dto.getCode() + "' already exists.");
                }

                createArea(dto);
                importedCount++;

            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
                errorMessages.add("Row " + (i + 1) + ": " + message);
            }
        }

        return new ImportUtil.ImportResult(importedCount, errorMessages);
    }

    public void updateArea(AreaDTO dto) {
        Area area = areaRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Area not found with ID: " + dto.getId()));

        mapToEntity(area, dto);
        areaRepository.save(area);
    }

    public void deleteArea(String id) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Area not found with ID: " + id));

        areaRepository.delete(area);
    }

    private void mapToEntity(Area area, AreaDTO dto) {
        area.setCode(dto.getCode().trim());
        area.setName(dto.getName().trim());
        area.setStatus(dto.getStatus());
        area.setDescription(dto.getDescription());

        // Map responsiblePerson
        if (dto.getResponsiblePerson() != null && dto.getResponsiblePerson().getEmployeeId() != null) {
            String empId = dto.getResponsiblePerson().getEmployeeId();

            User user = userRepository.findByEmployeeId4Roles(empId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with employeeId: " + empId));

            area.setResponsiblePerson(user);
        } else {
            throw new IllegalArgumentException("Responsible person is required");
        }
    }

    private UserDTO mapToUserDTO(User user) {
        if (user == null)
            return null;

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        return dto;
    }

    private AreaDTO toDTO(Area area) {
        AreaDTO dto = new AreaDTO();
        dto.setId(area.getId());
        dto.setCode(area.getCode());
        dto.setName(area.getName());
        dto.setStatus(area.getStatus());
        dto.setDescription(area.getDescription());

        dto.setResponsiblePerson(mapToUserDTO(area.getResponsiblePerson()));

        return dto;
    }
}

// /**
// * Update complaint status
// * If status is set to CLOSED:
// * - Sets closeTime
// * - Calculates total resolution time
// * - Deducts used parts from inventory
// */
// @Transactional
// public Complaint updateStatus(String complaintId, Complaint.Status newStatus)
// {
// log.info("Updating complaint ID: {} status to {}", complaintId, newStatus);

// Complaint complaint = complaintRepository.findById(complaintId)
// .orElseThrow(() -> new NoSuchElementException("Complaint not found: " +
// complaintId));

// Complaint.Status oldStatus = complaint.getStatus();
// complaint.setStatus(newStatus);

// if (newStatus == Complaint.Status.CLOSED && oldStatus !=
// Complaint.Status.CLOSED) {
// LocalDateTime now = LocalDateTime.now();
// complaint.setCloseTime(now);
// // totalResolutionTimeMinutes is calculated in @PreUpdate
// }

// complaintRepository.save(complaint);

// // Deduct inventory only when transitioning to CLOSED
// if (newStatus == Complaint.Status.CLOSED && oldStatus !=
// Complaint.Status.CLOSED) {
// deductPartsFromInventory(complaint);
// }

// if (newStatus == Complaint.Status.CLOSED && oldStatus !=
// Complaint.Status.CLOSED) {
// log.info("Complaint {} CLOSED: Deducting {} parts from inventory",
// complaintId, complaint.getPartsUsed().size());
// }

// return complaint;
// }

// /**
// * Deduct all parts used in this complaint from stock
// */
// private void deductPartsFromInventory(Complaint complaint) {
// for (ComplaintPart cp : complaint.getPartsUsed()) {
// log.info("Deducting {} x '{}' (Part ID: {}) from stock",
// cp.getQuantity(), cp.getPart().getName(), cp.getPart().getId());
// cp.getPart().useParts(cp.getQuantity());
// partRepository.save(cp.getPart());
// }
// }

// /**
// * Reopen a CLOSED complaint → restock parts
// */
// @Transactional
// public Complaint reopenComplaint(String complaintId) {
// log.warn("Reopening CLOSED complaint: {}", complaintId);
// Complaint complaint = complaintRepository.findById(complaintId)
// .orElseThrow(() -> new NoSuchElementException("Complaint not found: " +
// complaintId));

// if (complaint.getStatus() != Complaint.Status.CLOSED) {
// throw new IllegalArgumentException("Only CLOSED complaints can be
// reopened.");
// }

// // Restock all parts
// restockParts(complaint);

// complaint.setStatus(Complaint.Status.IN_PROGRESS);
// complaint.setCloseTime(null);
// complaint.setTotalResolutionTimeMinutes(null);

// log.info("Complaint {} reopened and {} parts restocked", complaintId,
// complaint.getPartsUsed().size());
// return complaintRepository.save(complaint);
// }

// private void restockParts(Complaint complaint) {
// for (ComplaintPart cp : complaint.getPartsUsed()) {
// Part part = cp.getPart();
// part.addStock(cp.getQuantity());
// partRepository.save(part);
// }
// }