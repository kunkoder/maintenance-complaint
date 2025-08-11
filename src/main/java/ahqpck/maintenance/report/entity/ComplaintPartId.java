package ahqpck.maintenance.report.entity;

import java.util.Objects;

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

    // @Override
    // public boolean equals(Object o) {
    //     if (this == o) return true;
    //     if (!(o instanceof ComplaintPartId)) return false;
    //     ComplaintPartId that = (ComplaintPartId) o;
    //     return Objects.equals(complaintId, that.complaintId) &&
    //            Objects.equals(partId, that.partId);
    // }

    // @Override
    // public int hashCode() {
    //     return Objects.hash(complaintId, partId);
    // }
}
