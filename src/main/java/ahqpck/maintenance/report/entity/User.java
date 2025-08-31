package ahqpck.maintenance.report.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"areas", "reportedComplaints", "assignedComplaints", "roles"})
@Getter
@Setter
public class User {

    @Id
    @Column(name = "id", length = 22, nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, name = "employee_id", nullable = false)
    @EqualsAndHashCode.Include
    private String employeeId;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String image;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "account_activation_token")
    private String accountActivationToken;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private LocalDateTime passwordResetTokenExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String designation;

    @Column(length = 50)
    private String nationality;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    // Relationships (excluded from equals/hashCode/toString)
    @OneToMany(mappedBy = "responsiblePerson", fetch = FetchType.LAZY)
    private final Set<Area> areas = new HashSet<>();

    @OneToMany(mappedBy = "reporter", fetch = FetchType.LAZY)
    private final Set<Complaint> reportedComplaints = new HashSet<>();

    @OneToMany(mappedBy = "assignee", fetch = FetchType.LAZY)
    private final Set<Complaint> assignedComplaints = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private final Set<Role> roles = new HashSet<>();

    public enum Status {
        ACTIVE, INACTIVE
    }

    @PrePersist
    public void prePersist() {
        this.id = this.id == null ? Base62.encode(UUID.randomUUID()) : this.id;
        this.createdAt = LocalDateTime.now();
        this.status = this.status != null ? this.status : Status.INACTIVE;
    }

    // Optional: override toString to avoid lazy loading
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                '}';
    }
}

// @Override
// public boolean equals(Object o) {
//     if (this == o) return true;
//     if (!(o instanceof User user)) return false;
//     return Objects.equals(id, user.id) &&
//            Objects.equals(employeeId, user.employeeId);
// }

// @Override
// public int hashCode() {
//     return Objects.hash(id, employeeId);
// }