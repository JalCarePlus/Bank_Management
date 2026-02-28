package banking_app.repository;

import banking_app.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    User findByEmail(String email);
    
    // For admin search functionality
    Page<User> findByUsernameContainingOrEmailContainingOrNameContaining(
        String username, String email, String name, Pageable pageable);
}
