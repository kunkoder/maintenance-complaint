package ahqpck.maintenance.report.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaDTO {

    private String id;

    @NotBlank(message = "Code is mandatory")
    private String code;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotNull(message = "Status is mandatory")
    private Status status;

    private String description;

    private UserDTO responsiblePerson;

    public enum Status {
        ACTIVE,
        INACTIVE,
        UNDER_MAINTENANCE
    }
}