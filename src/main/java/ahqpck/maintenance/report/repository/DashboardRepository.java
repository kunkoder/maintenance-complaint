package ahqpck.maintenance.report.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ahqpck.maintenance.report.dto.ComplaintStatusWeeklyDTO;
import ahqpck.maintenance.report.dto.EquipmentComplaintCountDTO;
import ahqpck.maintenance.report.dto.StatusCountDTO;
import ahqpck.maintenance.report.dto.UserComplaintSummaryDTO;
import ahqpck.maintenance.report.dto.UserDailySummaryDTO;
import ahqpck.maintenance.report.dto.UserWeeklySummaryDTO;
import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
public interface DashboardRepository extends JpaRepository<Complaint, String> {

    // All-time count grouped by status
    @Query("SELECT NEW ahqpck.maintenance.report.dto.StatusCountDTO(c.status, COUNT(c)) " +
            "FROM Complaint c " +
            "GROUP BY c.status " +
            "ORDER BY COUNT(c) DESC")
    List<StatusCountDTO> countByStatusAllTime();

    // Count by status in date range
    @Query("SELECT NEW ahqpck.maintenance.report.dto.StatusCountDTO(c.status, COUNT(c)) " +
            "FROM Complaint c " +
            "WHERE c.reportDate BETWEEN :start AND :end " +
            "GROUP BY c.status " +
            "ORDER BY COUNT(c) DESC")
    List<StatusCountDTO> countByStatusInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT NEW ahqpck.maintenance.report.dto.UserComplaintSummaryDTO(
                u.name,
                COUNT(c.id),
                SUM(CASE WHEN c.status = 'OPEN' THEN 1 ELSE 0 END),
                SUM(CASE WHEN c.status = 'CLOSED' THEN 1 ELSE 0 END),
                SUM(CASE WHEN c.status = 'PENDING' THEN 1 ELSE 0 END),
                SUM(CASE WHEN c.status = 'IN_PROGRESS' THEN 1 ELSE 0 END)
            )
            FROM User u
            LEFT JOIN Complaint c ON c.assignee = u
            GROUP BY u.id, u.name
            ORDER BY COUNT(c.id) DESC
            """)
    List<UserComplaintSummaryDTO> getUserComplaintSummary();

    @Query(value = """
            SELECT
                u.name AS user_name,
                CAST(COUNT(c.id) AS SIGNED) AS total_assigned_last_7_days,

                CAST(SUM(CASE WHEN DATE(c.report_date) = CURDATE() - INTERVAL 1 DAY THEN 1 ELSE 0 END) AS SIGNED) AS open_day_1,
                CAST(SUM(CASE WHEN DATE(c.report_date) = CURDATE() - INTERVAL 2 DAY THEN 1 ELSE 0 END) AS SIGNED) AS open_day_2,
                CAST(SUM(CASE WHEN DATE(c.report_date) = CURDATE() - INTERVAL 3 DAY THEN 1 ELSE 0 END) AS SIGNED) AS open_day_3,
                CAST(SUM(CASE WHEN DATE(c.report_date) = CURDATE() - INTERVAL 4 DAY THEN 1 ELSE 0 END) AS SIGNED) AS open_day_4,
                CAST(SUM(CASE WHEN DATE(c.report_date) = CURDATE() - INTERVAL 5 DAY THEN 1 ELSE 0 END) AS SIGNED) AS open_day_5,
                CAST(SUM(CASE WHEN DATE(c.report_date) = CURDATE() - INTERVAL 6 DAY THEN 1 ELSE 0 END) AS SIGNED) AS open_day_6,
                CAST(SUM(CASE WHEN DATE(c.report_date) = CURDATE() - INTERVAL 7 DAY THEN 1 ELSE 0 END) AS SIGNED) AS open_day_7,

                CAST(SUM(CASE WHEN DATE(c.close_time) = CURDATE() - INTERVAL 1 DAY THEN 1 ELSE 0 END) AS SIGNED) AS closed_day_1,
                CAST(SUM(CASE WHEN DATE(c.close_time) = CURDATE() - INTERVAL 2 DAY THEN 1 ELSE 0 END) AS SIGNED) AS closed_day_2,
                CAST(SUM(CASE WHEN DATE(c.close_time) = CURDATE() - INTERVAL 3 DAY THEN 1 ELSE 0 END) AS SIGNED) AS closed_day_3,
                CAST(SUM(CASE WHEN DATE(c.close_time) = CURDATE() - INTERVAL 4 DAY THEN 1 ELSE 0 END) AS SIGNED) AS closed_day_4,
                CAST(SUM(CASE WHEN DATE(c.close_time) = CURDATE() - INTERVAL 5 DAY THEN 1 ELSE 0 END) AS SIGNED) AS closed_day_5,
                CAST(SUM(CASE WHEN DATE(c.close_time) = CURDATE() - INTERVAL 6 DAY THEN 1 ELSE 0 END) AS SIGNED) AS closed_day_6,
                CAST(SUM(CASE WHEN DATE(c.close_time) = CURDATE() - INTERVAL 7 DAY THEN 1 ELSE 0 END) AS SIGNED) AS closed_day_7

            FROM users u
            LEFT JOIN complaints c ON u.employee_id = c.assignee
                AND (
                    c.report_date >= CURDATE() - INTERVAL 7 DAY
                    OR c.close_time >= CURDATE() - INTERVAL 7 DAY
                )
                AND c.report_date < CURDATE()  -- Exclude today
            GROUP BY u.id, u.name
            ORDER BY total_assigned_last_7_days DESC
            """, nativeQuery = true)
    List<UserWeeklySummaryDTO> getUserWeeklySummary(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT
                DATE_FORMAT(d.day, '%Y-%m-%d') AS date,
                CONCAT('Day-', ROW_NUMBER() OVER (ORDER BY d.day DESC)) AS dayLabel,

                CAST(SUM(CASE WHEN c.status = 'OPEN' THEN 1 ELSE 0 END) AS SIGNED) AS open,
                CAST(SUM(CASE WHEN c.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS SIGNED) AS inProgress,
                CAST(SUM(CASE WHEN c.status = 'PENDING' THEN 1 ELSE 0 END) AS SIGNED) AS pending,
                CAST(SUM(CASE WHEN c.status = 'DONE' THEN 1 ELSE 0 END) AS SIGNED) AS done,
                CAST(SUM(CASE WHEN c.status = 'CLOSED' THEN 1 ELSE 0 END) AS SIGNED) AS closed

            FROM (
                SELECT
                    DATE_ADD(
                        COALESCE(:from, DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
                        INTERVAL (units.a + tens.a * 10) DAY
                    ) AS day
                FROM
                    (SELECT 0 AS a UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
                     UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
                     UNION ALL SELECT 8 UNION ALL SELECT 9) units
                    CROSS JOIN
                    (SELECT 0 AS a UNION ALL SELECT 1) tens
            ) d
            LEFT JOIN complaints c ON DATE(c.report_date) = d.day
            WHERE
                d.day >= COALESCE(:from, DATE_SUB(CURDATE(), INTERVAL 7 DAY))
                AND d.day < COALESCE(:to, CURDATE())
                AND d.day <= CURDATE()
            GROUP BY d.day
            ORDER BY d.day DESC
            """, nativeQuery = true)
    List<ComplaintStatusWeeklyDTO> getComplaintStatusWeekly(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
    SELECT
        u.name AS user_name,
        DATE(d.day) AS date,  -- Extract date part
        CAST(SUM(CASE WHEN DATE(c.report_date) = DATE(d.day) THEN 1 ELSE 0 END) AS SIGNED) AS openCount,
        CAST(SUM(CASE WHEN DATE(c.close_time) = DATE(d.day) THEN 1 ELSE 0 END) AS SIGNED) AS closedCount
    FROM (
        -- Generate all dates between :from and :to (truncated to day)
        SELECT DATE_ADD(
            COALESCE(DATE(:from), DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
            INTERVAL (units.a + tens.a * 10) DAY
        ) AS day
        FROM
            (SELECT 0 AS a UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
             UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
             UNION ALL SELECT 8 UNION ALL SELECT 9) units
        CROSS JOIN
            (SELECT 0 AS a UNION ALL SELECT 1) tens
    ) d
    CROSS JOIN users u
    LEFT JOIN complaints c ON u.employee_id = c.assignee
        AND (
            DATE(c.report_date) = DATE(d.day) OR
            DATE(c.close_time) = DATE(d.day)
        )
    WHERE
        DATE(d.day) >= COALESCE(DATE(:from), DATE_SUB(CURDATE(), INTERVAL 7 DAY))
        AND DATE(d.day) < COALESCE(DATE(:to), CURDATE())
        AND DATE(d.day) <= CURDATE()
    GROUP BY u.id, u.name, DATE(d.day)
    ORDER BY u.name, DATE(d.day) DESC
    """, nativeQuery = true)
List<UserDailySummaryDTO> getUserDailySummary(
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
);

    @Query(value = """
            SELECT
                e.name AS equipment_name,
                e.code AS equipment_code,
                CAST(COUNT(c.id) AS SIGNED) AS total_complaints
            FROM equipments e
            LEFT JOIN complaints c ON e.code = c.equipment_code
            GROUP BY e.id, e.code, e.name
            ORDER BY total_complaints DESC
            """, nativeQuery = true)
    List<EquipmentComplaintCountDTO> getEquipmentComplaintCount();
}