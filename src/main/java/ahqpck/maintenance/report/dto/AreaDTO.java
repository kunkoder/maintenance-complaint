package ahqpck.maintenance.report.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.Role;
import ahqpck.maintenance.report.entity.User;
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

    private Area.Status status;
    private String description;

    private UserDTO responsiblePerson;
}