package ahqpck.maintenance.report.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import ahqpck.maintenance.report.entity.Complaint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintDTO {

    private String id;

    // private String code;

    private LocalDateTime reportDate;

    private LocalDateTime updatedAt;

    private AreaDTO area;

    @NotNull(message = "Equipment is mandatory")
    private EquipmentDTO equipment;

    @NotNull(message = "Reporter is mandatory")
    private UserDTO reporter;

    private String subject;

    private String description;

    @NotNull(message = "Assignee is mandatory")
    private UserDTO assignee;

    @NotNull(message = "Priority is mandatory")
    private Complaint.Priority priority;

    @NotNull(message = "Category is mandatory")
    private Complaint.Category category;

    private Complaint.Status status;

    private String actionTaken;

    private String imageBefore;
    private String imageAfter;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime closeTime;

    private Integer totalResolutionTimeMinutes;

    private String resolutionTimeDisplay;

    @Valid
    private List<ComplaintPartDTO> partsUsed = new ArrayList<>();
}