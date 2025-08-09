package ahqpck.maintenance.report.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintPartDTO {

    @Valid
    @NotNull(message = "Part is mandatory in complaint part")
    private PartDTO part;

    @Positive(message = "Quantity must be greater than zero")
    @NotNull(message = "Quantity is mandatory")
    private Integer quantity;
}