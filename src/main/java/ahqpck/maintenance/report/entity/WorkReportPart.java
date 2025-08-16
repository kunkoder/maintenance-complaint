package ahqpck.maintenance.report.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ahqpck.maintenance.report.util.Base62;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "work_report_parts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkReportPart {

    @EmbeddedId
    private WorkReportPartId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("workReportId")
    @JoinColumn(name = "work_report_id")
    private WorkReport workReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("partId")
    @JoinColumn(name = "part_id")
    private Part part;

    @Column(nullable = false)
    private Integer quantity;
}