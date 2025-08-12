package ahqpck.maintenance.report.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ahqpck.maintenance.report.entity.Complaint;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DashboardRepository {

    private final EntityManager entityManager;

    public List<Object[]> countComplaintsByStatus() {
        String jpql = "SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status";
        return entityManager.createQuery(jpql).getResultList();
    }

public List<Object[]> countComplaintsByAssigneeAndStatus(Complaint.Status status) {
    String jpql = "SELECT c.assignee.name, COUNT(c) " +
                  "FROM Complaint c " +
                  "WHERE c.status = :status " +
                  "GROUP BY c.assignee.name";
    return entityManager.createQuery(jpql)
            .setParameter("status", status)
            .getResultList();
}

    public List<Object[]> countComplaintsByArea() {
        String jpql = "SELECT a.name, COUNT(c) FROM Complaint c " +
                      "JOIN c.area a GROUP BY a.name ORDER BY COUNT(c) DESC";
        return entityManager.createQuery(jpql).getResultList();
    }

    public List<Object[]> countComplaintsLast7Days() {
    LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(6); // last 7 days including today

    String jpql = "SELECT " +
                  "FUNCTION('DAYOFWEEK', c.reportDate), " +
                  "c.status, " +
                  "COUNT(c) " +
                  "FROM Complaint c " +
                  "WHERE c.reportDate >= :startDate " +
                  "GROUP BY FUNCTION('DAYOFWEEK', c.reportDate), c.status";

    return entityManager.createQuery(jpql)
            .setParameter("startDate", sevenDaysAgo)
            .getResultList();
}

}