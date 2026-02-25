package banking_app.repository;

import banking_app.entity.Account;
import banking_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByAccountNumber(String accountNumber);
    Account findByUser(User user);
    
    // Optional methods
    List<Account> findByBalanceLessThan(Double threshold);
    
    @Query("SELECT SUM(a.balance) FROM Account a")
    Double getTotalBalance();
    
    long countByBalanceGreaterThan(Double threshold);
}