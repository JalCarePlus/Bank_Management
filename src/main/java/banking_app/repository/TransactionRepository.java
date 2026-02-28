package banking_app.repository;

import banking_app.entity.Transaction;
import banking_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUser(User user);
    
    List<Transaction> findTop10ByOrderByDateTimeDesc();
    
    @Query("SELECT FUNCTION('DATE', t.dateTime), COUNT(t) FROM Transaction t WHERE t.dateTime >= :since GROUP BY FUNCTION('DATE', t.dateTime)")
    List<Object[]> getDailyTransactionCount(@Param("since") LocalDateTime since);
    
    @Query("SELECT FUNCTION('DATE', t.dateTime), SUM(t.amount) FROM Transaction t WHERE t.dateTime >= :since GROUP BY FUNCTION('DATE', t.dateTime)")
    List<Object[]> getDailyTransactionVolume(@Param("since") LocalDateTime since);
}
