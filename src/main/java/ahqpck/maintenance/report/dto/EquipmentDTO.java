package ahqpck.maintenance.report.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentDTO {

    private String id;
    
    @NotBlank(message = "Code is mandatory")
    private String code;

    @NotBlank(message = "Name is mandatory")
    private String name;

    private String model;
    private String unit;
    private Integer qty = 0;

    private String manufacturer;
    private String serialNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate manufacturedDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate commissionedDate;

    private String capacity;
    private String remarks;

    private String image;
}
