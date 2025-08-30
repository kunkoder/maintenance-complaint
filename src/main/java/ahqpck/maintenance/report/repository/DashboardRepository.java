package ahqpck.maintenance.report.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ahqpck.maintenance.report.dto.DailyStatusCountDTO;
import ahqpck.maintenance.report.dto.EquipmentComplaintCountDTO;
import ahqpck.maintenance.report.dto.MonthlyStatusCountDTO;
import ahqpck.maintenance.report.dto.StatusCountDTO;
import ahqpck.maintenance.report.entity.Complaint;

@Repository
public interface DashboardRepository extends JpaRepository<Complaint, String> {

    @Query(value = """
            SELECT
                CAST(COALESCE((SELECT COUNT(*) FROM complaints), 0) AS SIGNED) AS totalComplaints,

                CAST(COALESCE(SUM(
                    CASE WHEN c.status IN ('OPEN', 'IN_PROGRESS')
                          AND DATE(c.report_date) >= DATE(:from)
                          AND DATE(c.report_date) < DATE(:to) THEN 1 ELSE 0 END
                ), 0) AS SIGNED) AS totalOpen,

                CAST(COALESCE(SUM(
                    CASE WHEN c.status IN ('DONE', 'CLOSED')
                          AND DATE(c.close_time) >= DATE(:from)
                          AND DATE(c.close_time) < DATE(:to) THEN 1 ELSE 0 END
                ), 0) AS SIGNED) AS totalClosed,

                CAST(COALESCE((SELECT COUNT(*) FROM complaints WHERE status = 'PENDING'), 0) AS SIGNED) AS totalPending

            FROM
                (SELECT 1) AS dummy
            LEFT JOIN complaints c
                ON (DATE(c.report_date) >= DATE(:from) AND DATE(c.report_date) < DATE(:to)
                    AND c.status IN ('OPEN', 'IN_PROGRESS'))
                OR (DATE(c.close_time) >= DATE(:from) AND DATE(c.close_time) < DATE(:to)
                    AND c.status IN ('DONE', 'CLOSED'))
            """, nativeQuery = true)
    StatusCountDTO getStatusCount(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT
                DATE_FORMAT(d.day, '%Y-%m-%d') AS date,

                -- Open: status = 'OPEN' AND reported on this day
                CAST(COALESCE((
                    SELECT COUNT(*)
                    FROM complaints c
                    WHERE c.status = 'OPEN'
                      AND DATE(c.report_date) = DATE(d.day)
                ), 0) AS SIGNED) AS open,

                -- Closed: status = 'CLOSED' AND closed on this day
                CAST(COALESCE((
                    SELECT COUNT(*)
                    FROM complaints c
                    WHERE c.status = 'CLOSED'
                      AND DATE(c.close_time) = DATE(d.day)
                ), 0) AS SIGNED) AS closed,

                -- Pending: status = 'PENDING' AND reported on this day
                CAST(COALESCE((
                    SELECT COUNT(*)
                    FROM complaints c
                    WHERE c.status = 'PENDING'
                      AND DATE(c.report_date) = DATE(d.day)
                ), 0) AS SIGNED) AS pending

            FROM (
                -- Generate continuous date range
                SELECT DATE_SUB(
                    COALESCE(:to, NOW()),
                    INTERVAL (units.a + tens.a * 10) DAY
                ) AS day
                FROM
                    (SELECT 0 AS a UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
                     UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
                     UNION ALL SELECT 8 UNION ALL SELECT 9) units
                    CROSS JOIN
                    (SELECT 0 AS a UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
                     UNION ALL SELECT 4 UNION ALL SELECT 5) tens
            ) d

            WHERE
                d.day >= COALESCE(:from, DATE_SUB(COALESCE(:to, NOW()), INTERVAL 6 DAY))
                AND d.day <= COALESCE(:to, NOW())
                AND d.day <= CURDATE()  -- Prevent future dates

            ORDER BY d.day ASC
            """, nativeQuery = true)
    List<DailyStatusCountDTO> getDailyStatusCount(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT
                DATE_FORMAT(d.month_start, '%Y-%m') AS date,

                -- Open: status = 'OPEN' AND reported in this month
                CAST(COALESCE((
                    SELECT COUNT(*)
                    FROM complaints c
                    WHERE c.status = 'OPEN'
                      AND YEAR(c.report_date) = YEAR(d.month_start)
                      AND MONTH(c.report_date) = MONTH(d.month_start)
                ), 0) AS SIGNED) AS open,

                -- Closed: status = 'CLOSED' AND closed in this month
                CAST(COALESCE((
                    SELECT COUNT(*)
                    FROM complaints c
                    WHERE c.status = 'CLOSED'
                      AND YEAR(c.close_time) = YEAR(d.month_start)
                      AND MONTH(c.close_time) = MONTH(d.month_start)
                ), 0) AS SIGNED) AS closed,

                -- Pending: status = 'PENDING' AND reported in this month
                CAST(COALESCE((
                    SELECT COUNT(*)
                    FROM complaints c
                    WHERE c.status = 'PENDING'
                      AND YEAR(c.report_date) = YEAR(d.month_start)
                      AND MONTH(c.report_date) = MONTH(d.month_start)
                ), 0) AS SIGNED) AS pending

            FROM (
                -- Generate 12 months: Jan to Dec of the target year
                SELECT DATE_ADD(
                    CONCAT(COALESCE(YEAR(:year), YEAR(NOW())), '-01-01'),
                    INTERVAL (units.a + tens.a * 10) MONTH
                ) AS month_start
                FROM
                    (SELECT 0 AS a UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
                     UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
                     UNION ALL SELECT 8 UNION ALL SELECT 9) units
                    CROSS JOIN
                    (SELECT 0 AS a UNION ALL SELECT 1) tens
            ) d

            WHERE
                YEAR(d.month_start) = COALESCE(YEAR(:year), YEAR(NOW()))
                AND d.month_start <= NOW()  -- Include current month even if partial

            ORDER BY d.month_start ASC
            """, nativeQuery = true)
    List<MonthlyStatusCountDTO> getMonthlyStatusCount(@Param("year") LocalDateTime year);

    @Query(value = """
            SELECT
                u.name AS assignee,
                c.status,
                DATE(c.report_date) AS report_date,
                COUNT(*) AS count
            FROM complaints c
            JOIN users u ON c.assignee = u.employee_id
            WHERE DATE(c.report_date) >= :from
              AND DATE(c.report_date) < DATE_ADD(:to, INTERVAL 1 DAY)
              AND c.status IN ('OPEN', 'PENDING', 'CLOSED')
            GROUP BY u.name, c.status, DATE(c.report_date)
            ORDER BY u.name, report_date
            """, nativeQuery = true)
    List<Object[]> getAssigneeDailyStatus(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

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