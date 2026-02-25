package banking_app.repository;

import banking_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    // Existing method
    User findByUsername(String username);
    
    // Search method with correct parameter names
    Page<User> findByUsernameContainingOrEmailContainingOrNameContaining(
        String username, String email, String name, Pageable pageable);
    
    // Daily registration statistics
    @Query("SELECT DATE(u.createdAt) as date, COUNT(u) as count FROM User u " +
           "WHERE u.createdAt >= :since GROUP BY DATE(u.createdAt) ORDER BY date")
    List<Object[]> getDailyRegistrations(@Param("since") LocalDateTime since);
    
    // Count active/inactive users
    long countByActive(boolean active);
}