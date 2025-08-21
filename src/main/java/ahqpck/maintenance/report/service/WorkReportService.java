package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AreaDTO;
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
import ahqpck.maintenance.report.util.ZeroPaddedCodeGenerator;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkReportService {

    private final WorkReportRepository workReportRepository;
    private final AreaRepository areaRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final Validator validator;
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
    public void createWorkReport(WorkReportDTO dto) {
        validateAndCreateWorkReport(dto, null);
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
                String generatedCode = codeGenerator.generate("WR");
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
                            () -> { throw new IllegalArgumentException("Area not found with code: " + areaCode); }
                    );
        } else {
            workReport.setArea(null);
        }

        // Equipment (required)
        String equipmentCode = dto.getEquipment().getCode();
        Equipment equipment = equipmentRepository.findByCode(equipmentCode)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found with code: " + equipmentCode));
        workReport.setEquipment(equipment);

        // Technician (required)
        String technicianEmpId = dto.getTechnician().getEmployeeId();
        User technician = userRepository.findByEmployeeId(technicianEmpId)
                .orElseThrow(() -> new IllegalArgumentException("Technician not found with employeeId: " + technicianEmpId));
        workReport.setTechnician(technician);

        // Supervisor (optional)
        if (dto.getSupervisor() != null && dto.getSupervisor().getEmployeeId() != null && !dto.getSupervisor().getEmployeeId().trim().isEmpty()) {
            String supervisorEmpId = dto.getSupervisor().getEmployeeId();
            User supervisor = userRepository.findByEmployeeId(supervisorEmpId)
                    .orElseThrow(() -> new IllegalArgumentException("Supervisor not found with employeeId: " + supervisorEmpId));
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
            if (days > 0) sb.append(days).append("d ");
            if (hours > 0) sb.append(hours).append("h ");
            if (mins > 0 || sb.length() == 0) sb.append(mins).append("m");
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
        dto.setTechnician(mapToUserDTO(workReport.getTechnician()));

        // Supervisor
        dto.setSupervisor(mapToUserDTO(workReport.getSupervisor()));

        return dto;
    }

    private UserDTO mapToUserDTO(User user) {
        if (user == null) return null;
        var dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        return dto;
    }
}