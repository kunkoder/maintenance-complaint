package ahqpck.maintenance.report.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ahqpck.maintenance.report.util.Base62;
import ahqpck.maintenance.report.util.ZeroPaddedIdGenerator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "complaints")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // @Column(nullable = false)
    // private String area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", referencedColumnName = "id", nullable = false)
    private Area area;

    // @Column(nullable = false)
    // private String machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", referencedColumnName = "id", nullable = false)
    private Equipment equipment;

    // @Column(nullable = false)
    // private String reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", referencedColumnName = "id", nullable = false)
    private User reporter;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String description;

    // @Column(nullable = true)
    // private String assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", referencedColumnName = "id", nullable = false)
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(name = "action_taken", columnDefinition = "TEXT", nullable = true)
    private String actionTaken;

    private String imageBefore;

    private String imageAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "close_time" , nullable = true)
    private LocalDateTime closeTime;

    @Column(name = "total_resolution_time_minutes", nullable = true)
    private Integer totalResolutionTimeMinutes;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ComplaintPart> partsUsed = new ArrayList<>();

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    public enum Category {
        MECHANICAL, ELECTRICAL, IT
    }

    public enum Status {
        OPEN, IN_PROGRESS, PENDING, CLOSED
    }

    public void addPart(Part part, Integer quantity) {
        ComplaintPart cp = new ComplaintPart();
        cp.setComplaint(this);
        cp.setPart(part);
        cp.setQuantity(quantity);
        cp.setId(new ComplaintPartId(this.id, part.getId()));
        this.partsUsed.add(cp);
    }

    public void removePart(Part part) {
        this.partsUsed.removeIf(cp -> cp.getPart().equals(part));
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            // this.id = ZeroPaddedIdGenerator.generate("CMP");
            this.id = Base62.encode(UUID.randomUUID());
        }
        LocalDateTime now = LocalDateTime.now();
        this.reportDate = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = Status.OPEN;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (Status.CLOSED.equals(this.status) && this.closeTime != null && this.reportDate != null) {
            this.totalResolutionTimeMinutes = (int) java.time.Duration
                    .between(this.reportDate, this.closeTime)
                    .toMinutes();
        }
    }
}



