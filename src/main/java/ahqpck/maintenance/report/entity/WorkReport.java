package ahqpck.maintenance.report.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import ahqpck.maintenance.report.util.Base62;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "work_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkReport {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Shift shift;

    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_code", referencedColumnName = "code", nullable = true)
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_code", referencedColumnName = "code", nullable = false)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String problem;

    @Column(name = "solution", columnDefinition = "TEXT", nullable = true)
    private String solution;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "stop_time", nullable = false)
    private LocalDateTime stopTime;

    @Column(name = "total_resolution_time_minutes", nullable = true)
    private Integer totalResolutionTimeMinutes;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "technician", referencedColumnName = "employee_id", nullable = false)
    // private User technician;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "work_report_technicians", joinColumns = @JoinColumn(name = "work_report_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private final Set<User> technicians = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor", referencedColumnName = "employee_id", nullable = true)
    private User supervisor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "work_type", nullable = true)
    private String workType;

    @OneToMany(mappedBy = "workReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkReportPart> partsUsed = new ArrayList<>();

    @Column(columnDefinition = "TEXT", nullable = true)
    private String remark;

    public enum Shift {
        DAY, NIGHT
    }

    // Enumerations
    public enum Category {
        CORRECTIVE_MAINTENANCE,
        PREVENTIVE_MAINTENANCE,
        BREAKDOWN,
        INSPECTION,
        MODIFICATION,
        INSTALLATION,
        OTHER
    }

    public enum Status {
        OPEN,
        PENDING,
        IN_PROGRESS,
        DONE,
        CLOSED
    }

    // Helper methods for part management
    public void addPart(Part part, Integer quantity) {
        WorkReportPart wrp = new WorkReportPart();
        wrp.setWorkReport(this);
        wrp.setPart(part);
        wrp.setQuantity(quantity);
        wrp.setId(new WorkReportPartId(this.id, part.getId()));
        this.partsUsed.add(wrp);
    }

    public void removePart(Part part) {
        this.partsUsed.removeIf(wrp -> wrp.getPart().equals(part));
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = Base62.encode(UUID.randomUUID());
        }

        if (this.code == null) {
            // Optional: implement code generation logic like WR-00001
            // this.code = ZeroPaddedIdGenerator.generate("WR");
        }

        LocalDateTime now = LocalDateTime.now();
        if (this.reportDate == null) {
            this.reportDate = now;
        }
        this.updatedAt = now;

        this.status = this.status != null ? this.status : Status.OPEN;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Add this setter
public void setTechnicians(Set<User> technicians) {
    this.technicians.clear();
    if (technicians != null) {
        this.technicians.addAll(technicians);
    }
}
}