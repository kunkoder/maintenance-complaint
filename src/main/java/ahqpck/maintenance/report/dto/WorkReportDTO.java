package ahqpck.maintenance.report.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.WorkReport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkReportDTO {

    private String id;

    private String code;

    private WorkReport.Shift shift;

    private LocalDateTime reportDate;

    private LocalDateTime updatedAt;

    private AreaDTO area;

    @NotNull(message = "Equipment is mandatory")
    private EquipmentDTO equipment;

    @NotNull(message = "Category is mandatory")
    private WorkReport.Category category;

    private String problem;

    private String solution;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime stopTime;

    @NotNull(message = "At least one technician is mandatory")
    @Size(min = 1, message = "At least one technician is required")
    private List<UserDTO> technicians = new ArrayList<>();

    private UserDTO supervisor;

    @NotNull(message = "Status is mandatory")
    private WorkReport.Status status;

    private String workType;

    private String remark;

    @Valid
    private List<WorkReportPartDTO> partsUsed = new ArrayList<>();

    private Integer totalResolutionTimeMinutes;

    private String resolutionTimeDisplay;
}