package banking_app.repository;

import banking_app.entity.Account;
import banking_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByUser(User user);
    Account findByAccountNumber(String accountNumber);
}
