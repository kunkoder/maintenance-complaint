package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.PartDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.dto.WorkReportDTO;
import ahqpck.maintenance.report.dto.WorkReportPartDTO;
import ahqpck.maintenance.report.entity.*;
import ahqpck.maintenance.report.repository.AreaRepository;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.repository.WorkReportRepository;
import ahqpck.maintenance.report.specification.WorkReportSpecification;
import ahqpck.maintenance.report.util.ZeroPaddedCodeGenerator;
import ahqpck.maintenance.report.util.ImportUtil;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.util.FileUploadUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkReportService {

    @Value("${app.upload-workreport-image-before.dir:src/main/resources/static/upload/workreport/image/before}")
    private String uploadDir;

    private static final Logger log = LoggerFactory.getLogger(WorkReportService.class);

    private final WorkReportRepository workReportRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;
    private final PartRepository partRepository;
    private final Validator validator;

    private final FileUploadUtil fileUploadUtil;
    private final ImportUtil importUtil;
    private final ZeroPaddedCodeGenerator codeGenerator;

    // ================== GET ALL WITH PAGINATION & SEARCH ==================
    @Transactional(readOnly = true)
    public Page<WorkReportDTO> getAllWorkReports(String keyword, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<WorkReport> spec = WorkReportSpecification.search(keyword);
        Page<WorkReport> workReportPage = workReportRepository.findAll(spec, pageable);

        return workReportPage.map(this::toDTO);
    }

    // ================== GET BY ID ==================
    @Transactional(readOnly = true)
    public WorkReportDTO getWorkReportById(String id) {
        WorkReport workReport = workReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Work report not found with ID: " + id));
        return toDTO(workReport);
    }

    // ================== CREATE ==================
    public void createWorkReport(WorkReportDTO dto, MultipartFile imageBefore) {
        // Validation is handled via DTO constraints and manual checks

        WorkReport workReport = new WorkReport();

        // Generate code if not provided
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            String generatedCode = codeGenerator.generate("WR");
            workReport.setCode(generatedCode);
        }

        mapToEntity(workReport, dto);

        workReportRepository.save(workReport);
    }

    // ================== IMPORT FROM EXCEL ==================
    public ImportUtil.ImportResult importWorkReportsFromExcel(List<Map<String, Object>> data) {
        List<String> errorMessages = new ArrayList<>();
        int importedCount = 0;

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data to import.");
        }

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                WorkReportDTO dto = new WorkReportDTO();

                // 🟡 Area (OPTIONAL)
                String areaCode = importUtil.toString(row.get("area"));
                if (areaCode != null && !areaCode.trim().isEmpty()) {
                    AreaDTO areaDTO = new AreaDTO();
                    areaDTO.setCode(areaCode.trim());
                    dto.setArea(areaDTO);
                }

                // ✅ Equipment (REQUIRED)
                String equipmentCode = importUtil.toString(row.get("equipment"));
                if (equipmentCode == null || equipmentCode.trim().isEmpty()) {
                    throw new IllegalArgumentException("Equipment is required");
                }
                EquipmentDTO equipmentDTO = new EquipmentDTO();
                equipmentDTO.setCode(equipmentCode.trim());
                dto.setEquipment(equipmentDTO);

                // ✅ Technician (REQUIRED)
                String technicianEmpId = importUtil.toString(row.get("technician"));
                if (technicianEmpId == null || technicianEmpId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Technician is required");
                }
                UserDTO technicianDTO = new UserDTO();
                technicianDTO.setEmployeeId(technicianEmpId.trim());
                dto.setTechnician(technicianDTO);

                // 🟡 Supervisor (OPTIONAL)
                String supervisorEmpId = importUtil.toString(row.get("supervisor"));
                if (supervisorEmpId != null && !supervisorEmpId.trim().isEmpty()) {
                    UserDTO supervisorDTO = new UserDTO();
                    supervisorDTO.setEmployeeId(supervisorEmpId.trim());
                    dto.setSupervisor(supervisorDTO);
                }

                // ✅ Category (REQUIRED)
                String categoryStr = importUtil.toString(row.get("category"));
                if (categoryStr == null || categoryStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Category is required");
                }
                try {
                    dto.setCategory(WorkReport.Category.valueOf(categoryStr.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid Category value: '" + categoryStr +
                                    "'. Must be one of: CORRECTIVE_MAINTENANCE, PREVENTIVE_MAINTENANCE, BREAKDOWN, etc.");
                }

                // ✅ Shift (REQUIRED)
                String shiftStr = importUtil.toString(row.get("shift"));
                if (shiftStr == null || shiftStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Shift is required");
                }
                try {
                    dto.setShift(WorkReport.Shift.valueOf(shiftStr.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid Shift value: '" + shiftStr + "'. Must be DAY or NIGHT.");
                }

                // Optional fields
                dto.setProblem(importUtil.toString(row.get("problem")));
                dto.setSolution(importUtil.toString(row.get("solution")));
                dto.setWorkType(importUtil.toString(row.get("work type")));
                dto.setRemark(importUtil.toString(row.get("remark")));

                // Status (optional)
                String statusStr = importUtil.toString(row.get("status"));
                if (statusStr != null && !statusStr.trim().isEmpty()) {
                    try {
                        dto.setStatus(WorkReport.Status.valueOf(statusStr.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid Status value: '" + statusStr + "'");
                    }
                }

                dto.setReportDate(importUtil.toLocalDateTime(row.get("report date")));
                dto.setStartTime(importUtil.toLocalDateTime(row.get("start time")));
                dto.setStopTime(importUtil.toLocalDateTime(row.get("stop time")));

                // Total resolution time (calculated if needed)
                Integer duration = importUtil.toDurationInMinutes(row.get("total time"));
                dto.setTotalResolutionTimeMinutes(duration);

                // Validate DTO
                Set<ConstraintViolation<WorkReportDTO>> violations = validator.validate(dto);
                if (!violations.isEmpty()) {
                    List<String> filteredMessages = violations.stream()
                            .filter(v -> !(v.getPropertyPath().toString().equals("supervisor")))
                            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .collect(Collectors.toList());

                    if (!filteredMessages.isEmpty()) {
                        throw new IllegalArgumentException("Validation failed: " + String.join(", ", filteredMessages));
                    }
                }

                createWorkReport(dto, null);
                importedCount++;

            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
                errorMessages.add("Row " + (i + 1) + ": " + message);
            }
        }

        return new ImportUtil.ImportResult(importedCount, errorMessages);
    }

    // ================== UPDATE ==================
    public void updateWorkReport(WorkReportDTO dto) {
        WorkReport workReport = workReportRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Work report not found with ID: " + dto.getId()));

        WorkReport.Status oldStatus = workReport.getStatus();
        WorkReport.Status newStatus = dto.getStatus();

        mapToEntity(workReport, dto);

        workReportRepository.save(workReport);
    }

    private void deductPartsFromInventory(WorkReport workReport) {
        for (WorkReportPart wrp : workReport.getPartsUsed()) {
            Part part = wrp.getPart();
            log.info("Deducting {} x '{}' (Part ID: {}) from stock",
                    wrp.getQuantity(), part.getName(), part.getId());
            part.useParts(wrp.getQuantity());
            partRepository.save(part);
        }
    }

    private void restockParts(WorkReport workReport) {
        for (WorkReportPart wrp : workReport.getPartsUsed()) {
            Part part = wrp.getPart();
            log.info("Restocking {} x '{}' (Part ID: {}) to inventory",
                    wrp.getQuantity(), part.getName(), part.getId());
            part.addStock(wrp.getQuantity());
            partRepository.save(part);
        }
    }

    // ================== DELETE ==================
    @Transactional
    public void deleteWorkReport(String id) {
        WorkReport workReport = workReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Work report not found with ID: " + id));

        workReportRepository.delete(workReport);
    }

    // ================== MAPPING METHODS ==================
    private void mapToEntity(WorkReport workReport, WorkReportDTO dto) {
        workReport.setShift(dto.getShift());
        workReport.setReportDate(dto.getReportDate());
        workReport.setProblem(dto.getProblem());
        workReport.setSolution(dto.getSolution());
        workReport.setStartTime(dto.getStartTime());
        workReport.setStopTime(dto.getStopTime());
        workReport.setWorkType(dto.getWorkType());
        workReport.setRemark(dto.getRemark());
        workReport.setStatus(dto.getStatus());
        workReport.setTotalResolutionTimeMinutes(dto.getTotalResolutionTimeMinutes());

        // Map Area
        if (dto.getArea() != null && dto.getArea().getCode() != null && !dto.getArea().getCode().trim().isEmpty()) {
            String areaCode = dto.getArea().getCode().trim();
            Area area = areaRepository.findByCode(areaCode)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Area not found with code: " + areaCode));
            workReport.setArea(area);
        } else {
            workReport.setArea(null);
        }

        // Map Equipment
        Equipment equipment = equipmentRepository.findByCode(dto.getEquipment().getCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Equipment not found with code: " + dto.getEquipment().getCode()));
        workReport.setEquipment(equipment);

        // Map Technician
        User technician = userRepository.findByEmployeeId(dto.getTechnician().getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Technician not found with employeeId: " + dto.getTechnician().getEmployeeId()));
        workReport.setTechnician(technician);

        // Map Supervisor (optional)
        if (dto.getSupervisor() != null && dto.getSupervisor().getEmployeeId() != null && !dto.getSupervisor().getEmployeeId().trim().isEmpty()) {
            User supervisor = userRepository.findByEmployeeId(dto.getSupervisor().getEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Supervisor not found with employeeId: " + dto.getSupervisor().getEmployeeId()));
            workReport.setSupervisor(supervisor);
        } else {
            workReport.setSupervisor(null);
        }

        // ✅ PARTS HANDLING
        if (dto.getPartsUsed() != null) {
            List<WorkReportPart> existingParts = new ArrayList<>(workReport.getPartsUsed());
            workReport.getPartsUsed().clear();

            for (WorkReportPartDTO partDto : dto.getPartsUsed()) {
                Part part = partRepository.findById(partDto.getPart().getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Part not found with ID: " + partDto.getPart().getId()));

                WorkReportPart existing = existingParts.stream()
                        .filter(wrp -> wrp.getPart().getId().equals(part.getId()))
                        .findFirst()
                        .orElse(null);

                WorkReportPart wrp;
                if (existing != null) {
                    existing.setQuantity(partDto.getQuantity());
                    wrp = existing;
                } else {
                    wrp = new WorkReportPart();
                    wrp.setWorkReport(workReport);
                    wrp.setPart(part);
                    wrp.setQuantity(partDto.getQuantity());
                    wrp.setId(new WorkReportPartId(workReport.getId(), part.getId()));
                }

                workReport.getPartsUsed().add(wrp);
            }
        } else {
            workReport.getPartsUsed().clear();
        }
    }

    // ================== HELPER: DTO Conversion ==================
    private WorkReportDTO toDTO(WorkReport workReport) {
        WorkReportDTO dto = new WorkReportDTO();
        dto.setId(workReport.getId());
        dto.setCode(workReport.getCode());
        dto.setShift(workReport.getShift());
        dto.setReportDate(workReport.getReportDate());
        dto.setUpdatedAt(workReport.getUpdatedAt());
        dto.setProblem(workReport.getProblem());
        dto.setSolution(workReport.getSolution());
        dto.setStartTime(workReport.getStartTime());
        dto.setStopTime(workReport.getStopTime());
        dto.setWorkType(workReport.getWorkType());
        dto.setRemark(workReport.getRemark());
        dto.setStatus(workReport.getStatus());
        dto.setTotalResolutionTimeMinutes(workReport.getTotalResolutionTimeMinutes());

        if (workReport.getTotalResolutionTimeMinutes() == null) {
            dto.setResolutionTimeDisplay("-");
        } else {
            int totalMinutes = workReport.getTotalResolutionTimeMinutes();
            int days = totalMinutes / (24 * 60);
            int remainingAfterDays = totalMinutes % (24 * 60);
            int hours = remainingAfterDays / 60;
            int minutes = remainingAfterDays % 60;

            StringBuilder display = new StringBuilder();
            if (days > 0) display.append(days).append("d ");
            if (hours > 0) display.append(hours).append("h ");
            if (minutes > 0 || display.length() == 0) display.append(minutes).append("m");

            dto.setResolutionTimeDisplay(display.toString().trim());
        }

        if (workReport.getArea() != null) {
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setId(workReport.getArea().getId());
            areaDTO.setCode(workReport.getArea().getCode());
            areaDTO.setName(workReport.getArea().getName());
            dto.setArea(areaDTO);
        }

        if (workReport.getEquipment() != null) {
            EquipmentDTO equipmentDTO = new EquipmentDTO();
            equipmentDTO.setId(workReport.getEquipment().getId());
            equipmentDTO.setName(workReport.getEquipment().getName());
            equipmentDTO.setCode(workReport.getEquipment().getCode());
            dto.setEquipment(equipmentDTO);
        }

        dto.setTechnician(mapToUserDTO(workReport.getTechnician()));
        dto.setSupervisor(mapToUserDTO(workReport.getSupervisor()));

        if (workReport.getPartsUsed() != null) {
            dto.setPartsUsed(workReport.getPartsUsed().stream()
                    .map(wrp -> {
                        WorkReportPartDTO partDto = new WorkReportPartDTO();
                        partDto.setPart(mapToPartDTO(wrp.getPart()));
                        partDto.setQuantity(wrp.getQuantity());
                        return partDto;
                    })
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private UserDTO mapToUserDTO(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        return dto;
    }

    private PartDTO mapToPartDTO(Part part) {
        if (part == null) return null;
        PartDTO dto = new PartDTO();
        dto.setId(part.getId());
        dto.setName(part.getName());
        dto.setCode(part.getCode());
        dto.setDescription(part.getDescription());
        return dto;
    }
}