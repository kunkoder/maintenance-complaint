package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.ComplaintDTO;
import ahqpck.maintenance.report.dto.ComplaintPartDTO;
import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.PartDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.*;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.AreaRepository;
import ahqpck.maintenance.report.repository.ComplaintRepository;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.specification.ComplaintSpecification;
import ahqpck.maintenance.report.util.FileUploadUtil;
import ahqpck.maintenance.report.util.ImportUtil;
import ahqpck.maintenance.report.util.ZeroPaddedCodeGenerator;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    @Value("${app.upload-complaint-image-before.dir:src/main/resources/static/upload/equipment/image/before}")
    private String uploadDir;

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;
    private final PartRepository partRepository;
    private final Validator validator;

    private final FileUploadUtil fileUploadUtil;
    private final ImportUtil importUtil;
    private final ZeroPaddedCodeGenerator codeGenerator;

    // ================== GET ALL WITH PAGINATION & SEARCH ==================
    // @Transactional(readOnly = true)
    public Page<ComplaintDTO> getAllComplaints(String keyword, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Complaint> spec = ComplaintSpecification.search(keyword);
        Page<Complaint> complaintPage = complaintRepository.findAll(spec, pageable);

        return complaintPage.map(this::toDTO);
    }

    // ================== GET BY ID ==================
    // @Transactional(readOnly = true)
    public ComplaintDTO getComplaintById(String id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found with ID: " + id));
        return toDTO(complaint);
    }

    // ================== CREATE ==================

    public void createComplaint(ComplaintDTO dto, MultipartFile imageBefore) {
        // validateDTO(dto);

        Complaint complaint = new Complaint();

        // Generate code before mapping
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            String generatedCode = codeGenerator.generate("CP");
            complaint.setCode(generatedCode);
        }

        mapToEntity(complaint, dto);

        if (imageBefore != null && !imageBefore.isEmpty()) {
            try {
                String fileName = fileUploadUtil.saveFile(uploadDir, imageBefore, "image");
                complaint.setImageBefore(fileName);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        complaintRepository.save(complaint);
    }

    // Add this method to EquipmentService
    public ImportUtil.ImportResult importComplaintsFromExcel(List<Map<String, Object>> data) {
        List<String> errorMessages = new ArrayList<>();
        int importedCount = 0;

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data to import.");
        }

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                ComplaintDTO dto = new ComplaintDTO();

                // 🟡 Area (OPTIONAL)
                String areaCode = importUtil.toString(row.get("area"));
                if (areaCode != null && !areaCode.trim().isEmpty()) {
                    AreaDTO areaDTO = new AreaDTO();
                    areaDTO.setCode(areaCode.trim());
                    dto.setArea(areaDTO);
                }
                // If area is missing or blank, leave it null

                // ✅ Equipment (REQUIRED)
                String equipmentCode = importUtil.toString(row.get("equipment"));
                if (equipmentCode == null || equipmentCode.trim().isEmpty()) {
                    throw new IllegalArgumentException("Equipment is required");
                }
                EquipmentDTO equipmentDTO = new EquipmentDTO();
                equipmentDTO.setCode(equipmentCode.trim());
                dto.setEquipment(equipmentDTO);

                // ✅ Reporter (REQUIRED)
                String reporterEmpId = importUtil.toString(row.get("reporter"));
                if (reporterEmpId == null || reporterEmpId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Reporter is required");
                }
                UserDTO reporterDTO = new UserDTO();
                reporterDTO.setEmployeeId(reporterEmpId.trim());
                dto.setReporter(reporterDTO);

                // ✅ Assignee (REQUIRED)
                String assigneeEmpId = importUtil.toString(row.get("assignee"));
                if (assigneeEmpId == null || assigneeEmpId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Assignee is required");
                }
                UserDTO assigneeDTO = new UserDTO();
                assigneeDTO.setEmployeeId(assigneeEmpId.trim());
                dto.setAssignee(assigneeDTO);

                // 🟡 Priority (OPTIONAL)
                String priorityStr = importUtil.toString(row.get("priority"));
                if (priorityStr != null && !priorityStr.trim().isEmpty()) {
                    try {
                        dto.setPriority(Complaint.Priority.valueOf(priorityStr.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                                "Invalid Priority value: '" + priorityStr
                                        + "'. Must be one of: LOW, MEDIUM, HIGH, or leave blank");
                    }
                }
                // If priority is missing or invalid → remains null

                // ✅ Category (REQUIRED)
                String categoryStr = importUtil.toString(row.get("category"));
                if (categoryStr == null || categoryStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Category is required");
                }
                try {
                    dto.setCategory(Complaint.Category.valueOf(categoryStr.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid Category value: '" + categoryStr
                                    + "'. Must be one of: MECHANICAL, ELECTRICAL, IT");
                }

                // Optional fields
                dto.setSubject(importUtil.toString(row.get("subject")));
                dto.setDescription(importUtil.toString(row.get("description")));

                // Status (optional)
                String statusStr = importUtil.toString(row.get("status"));
                if (statusStr != null && !statusStr.trim().isEmpty()) {
                    try {
                        dto.setStatus(Complaint.Status.valueOf(statusStr.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid Status value: '" + statusStr + "'");
                    }
                }
                // Else keep null (default behavior)

                dto.setActionTaken(importUtil.toString(row.get("action taken")));
                System.out.println("action taken " + row.get("action taken"));
                dto.setReportDate(importUtil.toLocalDateTime(row.get("report date")));
                System.out.println("date report " + importUtil.toLocalDateTime(row.get("report date")));
                dto.setCloseTime(importUtil.toLocalDateTime(row.get("close time")));
                dto.setTotalResolutionTimeMinutes(importUtil.toDurationInMinutes(row.get("total time")));

                // Final validation — BUT exclude fields that are now optional
                Set<ConstraintViolation<ComplaintDTO>> violations = validator.validate(dto);
                if (!violations.isEmpty()) {
                    // Filter out violations for fields we now allow to be null (e.g., priority,
                    // area)
                    List<String> filteredMessages = violations.stream()
                            .filter(v -> !(v.getPropertyPath().toString().equals("priority") ||
                                    v.getPropertyPath().toString().equals("area")))
                            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .collect(Collectors.toList());

                    if (!filteredMessages.isEmpty()) {
                        throw new IllegalArgumentException("Validation failed: " + String.join(", ", filteredMessages));
                    }
                    // Otherwise, proceed if only ignored fields had issues
                }

                createComplaint(dto, null);
                importedCount++;

            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
                errorMessages.add("Row " + (i + 1) + ": " + message);
            }
        }

        return new ImportUtil.ImportResult(importedCount, errorMessages);
    }

    public void updateComplaint(ComplaintDTO dto) {
        Complaint complaint = complaintRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Complaint not found with ID: " + dto.getId()));

        Complaint.Status oldStatus = complaint.getStatus();
        Complaint.Status newStatus = dto.getStatus();

        // Map all other fields first
        mapToEntity(complaint, dto);

        // Handle status transitions
        if (newStatus != null && newStatus != oldStatus) {
            handleStatusTransition(complaint, oldStatus, newStatus);
        }

        complaintRepository.save(complaint);
    }

    /**
     * Handle side effects of status transitions:
     * - Closing: set closeTime, deduct inventory
     * - Reopening: clear closeTime, restock parts
     */

    protected void handleStatusTransition(Complaint complaint, Complaint.Status oldStatus, Complaint.Status newStatus) {
        if (newStatus == Complaint.Status.CLOSED && oldStatus != Complaint.Status.CLOSED) {
            // Transitioning TO CLOSED
            LocalDateTime now = LocalDateTime.now();
            complaint.setCloseTime(now);
            // totalResolutionTimeMinutes will be calculated in @PreUpdate or @PrePersist

            log.info("Complaint {} CLOSED: Deducting {} parts from inventory",
                    complaint.getId(), complaint.getPartsUsed().size());
            deductPartsFromInventory(complaint);

        } else if (oldStatus == Complaint.Status.CLOSED && newStatus != Complaint.Status.CLOSED) {
            // Reopening a CLOSED complaint
            log.warn("Reopening CLOSED complaint: {}", complaint.getId());
            restockParts(complaint);

            complaint.setCloseTime(null);
            complaint.setTotalResolutionTimeMinutes(null);
            complaint.setStatus(newStatus); // Allow transition to any non-CLOSED
        }
        // For other transitions (e.g. OPEN → IN_PROGRESS), no side effects
    }

    /**
     * Deduct all parts used in this complaint from stock
     */
    private void deductPartsFromInventory(Complaint complaint) {
        for (ComplaintPart cp : complaint.getPartsUsed()) {
            Part part = cp.getPart();
            log.info("Deducting {} x '{}' (Part ID: {}) from stock",
                    cp.getQuantity(), part.getName(), part.getId());
            part.useParts(cp.getQuantity());
            partRepository.save(part);
        }
    }

    /**
     * Restock all parts used in this complaint
     */
    private void restockParts(Complaint complaint) {
        for (ComplaintPart cp : complaint.getPartsUsed()) {
            Part part = cp.getPart();
            log.info("Restocking {} x '{}' (Part ID: {}) to inventory",
                    cp.getQuantity(), part.getName(), part.getId());
            part.addStock(cp.getQuantity());
            partRepository.save(part);
        }
    }

    // ================== DELETE ==================
    @Transactional
    public void deleteComplaint(String id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found with ID: " + id));

        complaintRepository.delete(complaint);
    }

    // ================== MAPPING METHODS ==================

    private void mapToEntity(Complaint complaint, ComplaintDTO dto) {
        complaint.setSubject(dto.getSubject());
        complaint.setDescription(dto.getDescription());
        complaint.setPriority(dto.getPriority());
        complaint.setStatus(dto.getStatus());
        complaint.setCategory(dto.getCategory());
        complaint.setActionTaken(dto.getActionTaken());
        complaint.setReportDate(dto.getReportDate());
        complaint.setCloseTime(dto.getCloseTime());
        complaint.setTotalResolutionTimeMinutes(dto.getTotalResolutionTimeMinutes());

        // Map Area
        if (dto.getArea() != null && dto.getArea().getCode() != null && !dto.getArea().getCode().trim().isEmpty()) {
            String areaCode = dto.getArea().getCode().trim();
            Area area = areaRepository.findByCode(areaCode)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Area not found with code: " + areaCode));
            complaint.setArea(area);
        } else {
            complaint.setArea(null);
        }

        // Map Equipment
        Equipment equipment = equipmentRepository.findByCode(dto.getEquipment().getCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Equipment not found with code: " + dto.getEquipment().getCode()));
        complaint.setEquipment(equipment);

        User reporter = userRepository.findByEmployeeId(dto.getReporter().getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reporter not found with employeeId: " + dto.getReporter().getEmployeeId()));
        complaint.setReporter(reporter);

        User assignee = userRepository.findByEmployeeId(dto.getAssignee().getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignee not found with employeeId: " + dto.getAssignee().getEmployeeId()));
        complaint.setAssignee(assignee);

        // ✅ PARTS HANDLING: Use merge/update pattern
        if (dto.getPartsUsed() != null) {
            // Create a copy of current parts to allow safe iteration
            List<ComplaintPart> existingParts = new ArrayList<>(complaint.getPartsUsed());

            // Clear the list — thanks to orphanRemoval, old entries will be deleted
            complaint.getPartsUsed().clear();

            for (ComplaintPartDTO partDto : dto.getPartsUsed()) {
                Part part = partRepository.findById(partDto.getPart().getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Part not found with ID: " + partDto.getPart().getId()));

                // Try to reuse an existing ComplaintPart if possible
                ComplaintPart existing = existingParts.stream()
                        .filter(cp -> cp.getPart().getId().equals(part.getId()))
                        .findFirst()
                        .orElse(null);

                ComplaintPart cp;
                if (existing != null) {
                    // Reuse and update quantity
                    existing.setQuantity(partDto.getQuantity());
                    cp = existing;
                } else {
                    // Create new
                    cp = new ComplaintPart();
                    cp.setComplaint(complaint);
                    cp.setPart(part);
                    cp.setQuantity(partDto.getQuantity());
                    cp.setId(new ComplaintPartId(complaint.getId(), part.getId()));
                }

                complaint.getPartsUsed().add(cp);
            }
        } else {
            // If DTO has no parts, just clear
            complaint.getPartsUsed().clear();
        }
    }

    // ================== HELPER: DTO Conversion ==================
    private ComplaintDTO toDTO(Complaint complaint) {
        ComplaintDTO dto = new ComplaintDTO();
        dto.setId(complaint.getId());
        dto.setCode(complaint.getCode());
        dto.setReportDate(complaint.getReportDate());
        dto.setUpdatedAt(complaint.getUpdatedAt());
        dto.setSubject(complaint.getSubject());
        dto.setDescription(complaint.getDescription());
        dto.setPriority(complaint.getPriority());
        dto.setCategory(complaint.getCategory());
        dto.setStatus(complaint.getStatus());
        dto.setActionTaken(complaint.getActionTaken());
        dto.setImageBefore(complaint.getImageBefore());
        dto.setImageAfter(complaint.getImageAfter());
        dto.setCloseTime(complaint.getCloseTime());
        dto.setTotalResolutionTimeMinutes(complaint.getTotalResolutionTimeMinutes());

        if (complaint.getTotalResolutionTimeMinutes() == null) {
            dto.setResolutionTimeDisplay("-");
        } else {
            int totalMinutes = complaint.getTotalResolutionTimeMinutes();

            int days = totalMinutes / (24 * 60);
            int remainingAfterDays = totalMinutes % (24 * 60);
            int hours = remainingAfterDays / 60;
            int minutes = remainingAfterDays % 60;

            StringBuilder display = new StringBuilder();

            if (days > 0) {
                display.append(days).append("d ");
            }
            if (hours > 0) {
                display.append(hours).append("h ");
            }
            if (minutes > 0 || display.length() == 0) {
                display.append(minutes).append("m");
            }

            dto.setResolutionTimeDisplay(display.toString().trim());
        }

        // Map Area
        if (complaint.getArea() != null) {
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setId(complaint.getArea().getId());
            areaDTO.setCode(complaint.getArea().getCode());
            areaDTO.setName(complaint.getArea().getName());
            dto.setArea(areaDTO);
        }

        // Map Equipment
        if (complaint.getEquipment() != null) {
            EquipmentDTO equipmentDTO = new EquipmentDTO();
            equipmentDTO.setId(complaint.getEquipment().getId());
            equipmentDTO.setName(complaint.getEquipment().getName());
            equipmentDTO.setCode(complaint.getEquipment().getCode());
            dto.setEquipment(equipmentDTO);
        }

        // Map Reporter
        dto.setReporter(mapToUserDTO(complaint.getReporter()));

        // Map Assignee
        dto.setAssignee(mapToUserDTO(complaint.getAssignee()));

        // Map Parts Used
        if (complaint.getPartsUsed() != null) {
            dto.setPartsUsed(complaint.getPartsUsed().stream()
                    .map(cp -> {
                        ComplaintPartDTO partDto = new ComplaintPartDTO();
                        partDto.setPart(mapToPartDTO(cp.getPart()));
                        partDto.setQuantity(cp.getQuantity());
                        return partDto;
                    })
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    // ================== HELPER: UserDTO Mapping ==================
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

    // ================== HELPER: PartDTO Mapping ==================
    private PartDTO mapToPartDTO(Part part) {
        if (part == null)
            return null;

        PartDTO dto = new PartDTO();
        dto.setId(part.getId());
        dto.setName(part.getName());
        dto.setCode(part.getCode());
        dto.setDescription(part.getDescription());
        return dto;
    }

    // ================== VALIDATION ==================
    // private void validateDTO(ComplaintDTO dto) {
    // Set<ConstraintViolation<ComplaintDTO>> violations = validator.validate(dto);
    // if (!violations.isEmpty()) {
    // String errorMsg = violations.stream()
    // .map(v -> v.getMessage())
    // .collect(Collectors.joining(", "));
    // throw new IllegalArgumentException("Validation failed: " + errorMsg);
    // }
    // }
}

// package ahqpck.maintenance.report.service;

// import ahqpck.maintenance.report.entity.*;
// import ahqpck.maintenance.report.repository.ComplaintPartRepository;
// import ahqpck.maintenance.report.repository.ComplaintRepository;
// import ahqpck.maintenance.report.repository.PartRepository;
// import jakarta.transaction.Transactional;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.NoSuchElementException;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// @Service
// @RequiredArgsConstructor
// public class ComplaintService {

// private static final Logger log =
// LoggerFactory.getLogger(ComplaintService.class);

// private final ComplaintRepository complaintRepository;
// private final PartRepository partRepository;
// private final ComplaintPartRepository complaintPartRepository;

// /**
// * Create a new maintenance complaint
// */
// @Transactional
// public Complaint createComplaint(Complaint complaint) {
// log.info("Creating new complaint with subject: '{}' for machine: {}",
// complaint.getSubject(), complaint.getMachine());

// complaint.setStatus(Complaint.Status.OPEN);
// complaint.setCloseTime(null);
// complaint.setTotalResolutionTimeMinutes(null);
// Complaint saved = complaintRepository.save(complaint);

// log.info("Complaint created successfully with ID: {}", saved.getId());
// return saved;
// }

// /**
// * Add a part to an existing complaint (before closing)
// */
// @Transactional
// public void addPartToComplaint(String complaintId, String partCode, Integer
// quantity) {
// log.info("Adding part '{}' (qty: {}) to complaint ID: {}", partCode,
// quantity, complaintId);

// Complaint complaint = complaintRepository.findById(complaintId)
// .orElseThrow(() -> new NoSuchElementException("Complaint not found: " +
// complaintId));

// Part part = partRepository.findByCode(partCode)
// .orElseThrow(() -> new NoSuchElementException("Part not found: " +
// partCode));

// // Check if this part is already added
// boolean alreadyAdded = complaint.getPartsUsed().stream()
// .anyMatch(cp -> cp.getPart().getId().equals(part.getId()));

// if (alreadyAdded) {
// throw new IllegalArgumentException("Part is already added to this complaint.
// Update quantity instead.");
// }

// complaint.addPart(part, quantity);
// complaintRepository.save(complaint);
// log.info("Part '{}' (qty: {}) added to complaint {}", partCode, quantity,
// complaintId);
// }

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

// /**
// * Find complaint by ID
// */
// public Complaint getComplaintById(String complaintId) {
// return complaintRepository.findById(complaintId)
// .orElseThrow(() -> new NoSuchElementException("Complaint not found: " +
// complaintId));
// }

// /**
// * Get all complaints
// */
// public List<Complaint> getAllComplaints() {
// return complaintRepository.findAll();
// }

// /**
// * Get complaints by status
// */
// public List<Complaint> getComplaintsByStatus(Complaint.Status status) {
// return complaintRepository.findByStatus(status);
// }

// /**
// * Get complaints by machine
// */
// public List<Complaint> getComplaintsByMachine(String machine) {
// return complaintRepository.findByMachine(machine);
// }

// /**
// * Update assignee
// */
// @Transactional
// public Complaint updateAssignee(String complaintId, String assignee) {
// Complaint complaint = getComplaintById(complaintId);
// complaint.setAssignee(assignee);
// return complaintRepository.save(complaint);
// }
// }