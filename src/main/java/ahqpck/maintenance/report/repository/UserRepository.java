package ahqpck.maintenance.report.repository;

import ahqpck.maintenance.report.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.employeeId = :employeeId")
    Optional<User> findByEmployeeId4Roles(@Param("employeeId") String employeeId);

    @Query("SELECT u FROM User u WHERE u.employeeId = :employeeId")
    Optional<User> findByEmployeeId(@Param("employeeId") String employeeId);

    Optional<User> findByPasswordResetToken(String token);

    boolean existsByEmailIgnoringCase(String email);

    boolean existsByEmployeeIdIgnoringCase(String employeeId);
}