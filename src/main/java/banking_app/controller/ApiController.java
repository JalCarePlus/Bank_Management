package banking_app.controller;

import banking_app.entity.*;
import banking_app.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // ================= REGISTER =================
    @PostMapping("/register")
    public String register(@RequestBody User user) {

        if (userRepository.findByUsername(user.getUsername()) != null) {
            return "Username already exists";
        }

        user.setRole("USER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setBalance(1000.0);
        account.setAccountNumber(String.valueOf(
                1000000000L + (long)(Math.random() * 8999999999L)
        ));
        accountRepository.save(account);

        return "User registered successfully";
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public String login(@RequestBody User user, HttpSession session) {

        User dbUser = userRepository.findByUsername(user.getUsername());

        if (dbUser == null ||
            !passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            return "Invalid username or password";
        }

        session.setAttribute("loggedInUser", dbUser);
        return "Login successful";
    }

    // ================= TRANSFER =================
    @PostMapping("/transfer")
    public String transfer(@RequestParam String receiverAccount,
                           @RequestParam Double amount,
                           HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "Please login first";
        }

        Account sender = accountRepository.findByUser(loggedInUser);
        Account receiver = accountRepository.findByAccountNumber(receiverAccount);

        if (receiver == null) return "Receiver not found";
        if (sender.getBalance() < amount) return "Insufficient balance";

        sender.setBalance(sender.getBalance() - amount);
        receiver.setBalance(receiver.getBalance() + amount);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction debit = new Transaction();
        debit.setUser(loggedInUser);
        debit.setAmount(amount);
        debit.setType("DEBIT");
        debit.setReceiverAccount(receiverAccount);
        debit.setDateTime(LocalDateTime.now());
        transactionRepository.save(debit);

        Transaction credit = new Transaction();
        credit.setUser(receiver.getUser());
        credit.setAmount(amount);
        credit.setType("CREDIT");
        credit.setReceiverAccount(sender.getAccountNumber());
        credit.setDateTime(LocalDateTime.now());
        transactionRepository.save(credit);

        return "Transfer successful";
    }

    // ================= TRANSACTIONS =================
    @GetMapping("/transactions")
    public List<Transaction> transactions(HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) return List.of();

        return transactionRepository.findByUser(loggedInUser);
    }

    // ================= LOGOUT =================
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "Logged out successfully";
    }
}
