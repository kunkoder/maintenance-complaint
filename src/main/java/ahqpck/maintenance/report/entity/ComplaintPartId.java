package ahqpck.maintenance.report.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintPartId implements java.io.Serializable {
    private String complaintId;
    private String partId;
}
