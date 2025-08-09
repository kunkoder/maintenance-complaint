package ahqpck.maintenance.report.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @NotNull(message = "Report date is mandatory")
    private LocalDateTime reportDate;

    @NotNull(message = "Updated at is mandatory")
    private LocalDateTime updatedAt;

    @NotNull(message = "Area is mandatory")
    private AreaDTO area;

    @NotNull(message = "Equipment is mandatory")
    private EquipmentDTO equipment;

    @NotNull(message = "Reporter is mandatory")
    private UserDTO reporter;

    @NotBlank(message = "Subject is mandatory")
    private String subject;

    private String description;

    @NotNull(message = "Assignee is mandatory")
    private UserDTO assignee;

    @NotNull(message = "Priority is mandatory")
    private Complaint.Priority priority;

    @NotNull(message = "Category is mandatory")
    private Complaint.Category category;

    private String actionTaken;

    private String imageBefore;
    private String imageAfter;

    @NotNull(message = "Status is mandatory")
    private Complaint.Status status;

    private LocalDateTime closeTime;

    private Integer totalResolutionTimeMinutes;

    @Valid
    private List<ComplaintPartDTO> partsUsed = new ArrayList<>();
}