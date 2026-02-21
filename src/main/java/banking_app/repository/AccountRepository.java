package banking_app.repository;

import banking_app.entity.Account;
import banking_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByAccountNumber(String accountNumber);
    Account findByUser(User user); // <-- add this
}
