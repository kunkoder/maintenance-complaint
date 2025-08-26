package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.ComplaintDTO;
import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.dto.WorkReportDTO;
import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.entity.WorkReport;
import ahqpck.maintenance.report.repository.WorkReportRepository;
import ahqpck.maintenance.report.repository.AreaRepository;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.specification.WorkReportSpecification;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.util.ImportUtil;
import ahqpck.maintenance.report.util.ZeroPaddedCodeGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkReportService {

    private final WorkReportRepository workReportRepository;
    private final AreaRepository areaRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final Validator validator;
    private final ImportUtil importUtil;
    private final ZeroPaddedCodeGenerator codeGenerator;

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

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
    public void createWorkReport(WorkReportDTO dto) {

        WorkReport workReport = new WorkReport();

        // Generate code before mapping
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            String generatedCode = codeGenerator.generate(WorkReport.class, "code", "WR");
            workReport.setCode(generatedCode);
        }

        mapToEntity(workReport, dto);

        workReportRepository.save(workReport);

        // validateAndCreateWorkReport(dto, null);
    }

    // Add this method to EquipmentService
    public ImportUtil.ImportResult importWorkReportsFromExcel(List<Map<String, Object>> data) {
        List<String> errorMessages = new ArrayList<>();
        int importedCount = 0;
        System.out.println("data imported " + data);

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data to import.");
        }

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                WorkReportDTO dto = new WorkReportDTO();

                // ✅ REPORT DATE (required)
                Object reportDateObj = row.get("reportDate");
                if (reportDateObj == null) {
                    throw new IllegalArgumentException("Report Date is required");
                }
                LocalDateTime reportDate = importUtil.toLocalDateTime(reportDateObj);
                if (reportDate == null) {
                    throw new IllegalArgumentException("Invalid Report Date format");
                }
                dto.setReportDate(reportDate);

                // ✅ SHIFT (required)
                String shiftStr = importUtil.toString(row.get("shift"));
                if (shiftStr == null || shiftStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Shift is required");
                }
                try {
                    dto.setShift(WorkReport.Shift.valueOf(shiftStr.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid Shift value: '" + shiftStr + "'");
                }

                // 🟡 AREA (optional)
                String areaCode = importUtil.toString(row.get("area"));
                if (areaCode != null && !areaCode.trim().isEmpty()) {
                    AreaDTO areaDTO = new AreaDTO();
                    areaDTO.setCode(areaCode.trim());
                    dto.setArea(areaDTO);
                }

                // ✅ EQUIPMENT (required)
                String equipmentCode = importUtil.toString(row.get("equipment"));
                if (equipmentCode == null || equipmentCode.trim().isEmpty()) {
                    throw new IllegalArgumentException("Equipment is required");
                }
                EquipmentDTO equipmentDTO = new EquipmentDTO();
                equipmentDTO.setCode(equipmentCode.trim());
                dto.setEquipment(equipmentDTO);

                // ✅ CATEGORY (required)
                String categoryStr = importUtil.toString(row.get("category"));
                if (categoryStr == null || categoryStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Category is required");
                }
                try {
                    dto.setCategory(WorkReport.Category.valueOf(categoryStr.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid Category value: '" + categoryStr + "'");
                }

                // ✅ STATUS (required)
                String statusStr = importUtil.toString(row.get("status"));
                if (statusStr == null || statusStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Status is required");
                }
                try {
                    dto.setStatus(WorkReport.Status.valueOf(statusStr.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid Status value: '" + statusStr + "'");
                }

                // ✅ TECHNICIAN (required)
                // String technicianEmpId = importUtil.toString(row.get("technician"));
                // if (technicianEmpId == null || technicianEmpId.trim().isEmpty()) {
                // throw new IllegalArgumentException("Technician is required");
                // }
                // Replace single technician
                String technicianEmpId = importUtil.toString(row.get("technician"));
                if (technicianEmpId == null || technicianEmpId.trim().isEmpty()) {
                    throw new IllegalArgumentException("At least one technician is required");
                }

                // Split by comma (support multiple: "EMP001,EMP002")
                Set<String> technicianEmpIds = Arrays.stream(technicianEmpId.trim().split(","))
                        .map(String::trim)
                        .filter(id -> !id.isEmpty())
                        .collect(Collectors.toSet());

                Set<UserDTO> technicianDTOs = technicianEmpIds.stream()
                        .map(empId -> {
                            UserDTO technicianDTO = new UserDTO(); // ✅ Rename to avoid conflict
                            technicianDTO.setEmployeeId(empId);
                            return technicianDTO;
                        })
                        .collect(Collectors.toSet());

                dto.setTechnicians(technicianDTOs);

                // 🟡 SUPERVISOR (optional)
                String supervisorEmpId = importUtil.toString(row.get("supervisor"));
                if (supervisorEmpId != null && !supervisorEmpId.trim().isEmpty()) {
                    UserDTO supervisorDTO = new UserDTO();
                    supervisorDTO.setEmployeeId(supervisorEmpId.trim());
                    dto.setSupervisor(supervisorDTO);
                }

                // 🟡 PROBLEM (optional)
                dto.setProblem(importUtil.toString(row.get("problem")));

                // 🟡 SOLUTION (optional)
                dto.setSolution(importUtil.toString(row.get("solution")));

                // 🟡 START TIME (optional, but recommended)
                dto.setStartTime(importUtil.toLocalDateTime(row.get("startTime")));

                // 🟡 STOP TIME (optional, but recommended)
                dto.setStopTime(importUtil.toLocalDateTime(row.get("stopTime")));

                // 🟡 TOTAL RESOLUTION TIME (optional, can be calculated)
                dto.setTotalResolutionTimeMinutes(importUtil.toDurationInMinutes(row.get("totalTime")));

                // 🟡 WORK TYPE (optional)
                dto.setWorkType(importUtil.toString(row.get("work type")));

                // 🟡 REMARK (optional)
                dto.setRemark(importUtil.toString(row.get("remark")));

                // Final validation (exclude optional fields like area, supervisor, etc. if
                // needed)
                Set<ConstraintViolation<WorkReportDTO>> violations = validator.validate(dto);
                if (!violations.isEmpty()) {
                    List<String> filteredMessages = violations.stream()
                            .filter(v -> {
                                String field = v.getPropertyPath().toString();
                                return !List.of("area", "supervisor", "problem", "solution", "workType", "remark")
                                        .contains(field);
                            })
                            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .collect(Collectors.toList());

                    if (!filteredMessages.isEmpty()) {
                        throw new IllegalArgumentException("Validation failed: " + String.join(", ", filteredMessages));
                    }
                }

                createWorkReport(dto);
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

        validateAndCreateWorkReport(dto, workReport);
    }

    // ================== DELETE ==================
    @Transactional
    public void deleteWorkReport(String id) {
        WorkReport workReport = workReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Work report not found with ID: " + id));

        workReportRepository.delete(workReport);
    }

    // ================== PRIVATE HELPERS ==================

    private void validateAndCreateWorkReport(WorkReportDTO dto, WorkReport existing) {
        // Generate code if creating new
        if (existing == null) {
            existing = new WorkReport();
            if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
                String generatedCode = codeGenerator.generate(WorkReport.class, "code", "WR");
                existing.setCode(generatedCode);
            }
        }

        mapToEntity(existing, dto);
        workReportRepository.save(existing);
    }

    private void mapToEntity(WorkReport workReport, WorkReportDTO dto) {
        workReport.setShift(dto.getShift());
        workReport.setReportDate(dto.getReportDate());
        workReport.setProblem(dto.getProblem());
        workReport.setSolution(dto.getSolution());
        workReport.setCategory(dto.getCategory());
        workReport.setStartTime(dto.getStartTime());
        workReport.setStopTime(dto.getStopTime());
        workReport.setWorkType(dto.getWorkType());
        workReport.setRemark(dto.getRemark());
        workReport.setStatus(dto.getStatus());
        workReport.setTotalResolutionTimeMinutes(dto.getTotalResolutionTimeMinutes());

        // Area (optional)
        if (dto.getArea() != null && dto.getArea().getCode() != null && !dto.getArea().getCode().trim().isEmpty()) {
            String areaCode = dto.getArea().getCode().trim();
            areaRepository.findByCode(areaCode)
                    .ifPresentOrElse(
                            workReport::setArea,
                            () -> {
                                throw new IllegalArgumentException("Area not found with code: " + areaCode);
                            });
        } else {
            workReport.setArea(null);
        }

        // Equipment (required)
        String equipmentCode = dto.getEquipment().getCode();
        Equipment equipment = equipmentRepository.findByCode(equipmentCode)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found with code: " + equipmentCode));
        workReport.setEquipment(equipment);

        // Technician (required)
        if (dto.getTechnicians() == null || dto.getTechnicians().isEmpty()) {
            throw new IllegalArgumentException("At least one technician is required");
        }

        Set<User> technicianUsers = dto.getTechnicians().stream()
                .map(technicianDTO -> {
                    String empId = technicianDTO.getEmployeeId();
                    if (empId == null || empId.trim().isEmpty()) {
                        throw new IllegalArgumentException("Technician employee ID is required");
                    }
                    return userRepository.findByEmployeeId(empId.trim())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Technician not found with employeeId: " + empId));
                })
                .collect(Collectors.toSet());

        workReport.setTechnicians(technicianUsers);

        // Supervisor (optional)
        if (dto.getSupervisor() != null && dto.getSupervisor().getEmployeeId() != null
                && !dto.getSupervisor().getEmployeeId().trim().isEmpty()) {
            String supervisorEmpId = dto.getSupervisor().getEmployeeId();
            User supervisor = userRepository.findByEmployeeId(supervisorEmpId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Supervisor not found with employeeId: " + supervisorEmpId));
            workReport.setSupervisor(supervisor);
        } else {
            workReport.setSupervisor(null);
        }
    }

    // ================== DTO CONVERSION ==================
    private WorkReportDTO toDTO(WorkReport workReport) {
        WorkReportDTO dto = new WorkReportDTO();
        dto.setId(workReport.getId());
        dto.setCode(workReport.getCode());
        dto.setShift(workReport.getShift());
        dto.setReportDate(workReport.getReportDate());
        dto.setUpdatedAt(workReport.getUpdatedAt());
        dto.setProblem(workReport.getProblem());
        dto.setSolution(workReport.getSolution());
        dto.setCategory(workReport.getCategory());
        dto.setStartTime(workReport.getStartTime());
        dto.setStopTime(workReport.getStopTime());
        dto.setWorkType(workReport.getWorkType());
        dto.setRemark(workReport.getRemark());
        dto.setStatus(workReport.getStatus());
        dto.setTotalResolutionTimeMinutes(workReport.getTotalResolutionTimeMinutes());

        // Format resolution time
        if (workReport.getTotalResolutionTimeMinutes() != null) {
            int total = workReport.getTotalResolutionTimeMinutes();
            int days = total / (24 * 60), hours = (total % (24 * 60)) / 60, mins = total % 60;
            StringBuilder sb = new StringBuilder();
            if (days > 0)
                sb.append(days).append("d ");
            if (hours > 0)
                sb.append(hours).append("h ");
            if (mins > 0 || sb.length() == 0)
                sb.append(mins).append("m");
            dto.setResolutionTimeDisplay(sb.toString().trim());
        } else {
            dto.setResolutionTimeDisplay("-");
        }

        // Area
        if (workReport.getArea() != null) {
            var areaDto = new AreaDTO();
            areaDto.setId(workReport.getArea().getId());
            areaDto.setCode(workReport.getArea().getCode());
            areaDto.setName(workReport.getArea().getName());
            dto.setArea(areaDto);
        }

        // Equipment
        if (workReport.getEquipment() != null) {
            var equipDto = new EquipmentDTO();
            equipDto.setId(workReport.getEquipment().getId());
            equipDto.setCode(workReport.getEquipment().getCode());
            equipDto.setName(workReport.getEquipment().getName());
            dto.setEquipment(equipDto);
        }

        // Technician
        dto.setTechnicians(workReport.getTechnicians().stream()
                .map(this::mapToUserDTO)
                .collect(Collectors.toSet()));

        // Supervisor
        dto.setSupervisor(mapToUserDTO(workReport.getSupervisor()));

        return dto;
    }

    private UserDTO mapToUserDTO(User user) {
        if (user == null)
            return null;
        var dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        return dto;
    }
}