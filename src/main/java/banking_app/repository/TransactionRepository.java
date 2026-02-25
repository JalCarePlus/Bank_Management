package banking_app.repository;

import banking_app.entity.Transaction;
import banking_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Existing method
    List<Transaction> findByUser(User user);
    
    // Recent transactions
    List<Transaction> findTop10ByOrderByDateTimeDesc();
    
    // Find by type with pagination
    Page<Transaction> findByType(String type, Pageable pageable);
    
    // Find by user with pagination
    Page<Transaction> findByUser(User user, Pageable pageable);
    
    // Daily transaction count
    @Query("SELECT DATE(t.dateTime) as date, COUNT(t) as count FROM Transaction t " +
           "WHERE t.dateTime >= :since GROUP BY DATE(t.dateTime) ORDER BY date")
    List<Object[]> getDailyTransactionCount(@Param("since") LocalDateTime since);
    
    // Daily transaction volume
    @Query("SELECT DATE(t.dateTime) as date, SUM(t.amount) as total FROM Transaction t " +
           "WHERE t.dateTime >= :since GROUP BY DATE(t.dateTime) ORDER BY date")
    List<Object[]> getDailyTransactionVolume(@Param("since") LocalDateTime since);
}