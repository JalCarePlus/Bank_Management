package banking_app.controller;

import banking_app.entity.Account;
import banking_app.entity.User;
import banking_app.entity.Transaction;
import banking_app.repository.AccountRepository;
import banking_app.repository.UserRepository;
import banking_app.repository.TransactionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;


    // ================== Registration ==================
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(User user, Model model) {
        user.setRole("USER");  // default role
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);


        // Create account automatically with initial balance
        Account account = new Account();
        account.setUser(user);
        account.setBalance(1000.0); // initial balance
        account.setAccountNumber(generateAccountNumber());
        accountRepository.save(account);

        model.addAttribute("message", "Registration successful! Your Account Number: " + account.getAccountNumber());
        return "register";
    }

    private String generateAccountNumber() {
        long number = 1000000000L + (long) (Math.random() * 8999999999L);
        return String.valueOf(number);
    }

    // ================== Login ==================
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@ModelAttribute("user") User user, Model model, HttpSession session) {
        User existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser != null &&
        	    passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            // Store logged-in user in session
            session.setAttribute("loggedInUser", existingUser);

            // Fetch user's account
            Account account = accountRepository.findByUser(existingUser);
            model.addAttribute("user", existingUser);
            model.addAttribute("account", account);
            return "dashboard";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    // ================== Dashboard ==================
    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        Account account = accountRepository.findByUser(loggedInUser);
        model.addAttribute("user", loggedInUser);
        model.addAttribute("account", account);
        return "dashboard";
    }

 // ================== Transfer Money ==================
    @GetMapping("/transfer")
    public String showTransferPage(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        Account account = accountRepository.findByUser(loggedInUser);
        model.addAttribute("user", loggedInUser);
        model.addAttribute("account", account);
        return "transfer";
    }

    @PostMapping("/transfer")
    public String transferMoney(@RequestParam("receiverAccount") String receiverAccountNumber,
                                @RequestParam("amount") Double amount,
                                Model model,
                                HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Account senderAccount = accountRepository.findByUser(loggedInUser);
        Account receiverAccount = accountRepository.findByAccountNumber(receiverAccountNumber);

        if (receiverAccount == null) {
            model.addAttribute("message", "Receiver account not found!");
            model.addAttribute("account", senderAccount);
            model.addAttribute("user", loggedInUser);
            return "transfer";
        }

        if (senderAccount.getBalance() < amount) {
            model.addAttribute("message", "Insufficient balance!");
            model.addAttribute("account", senderAccount);
            model.addAttribute("user", loggedInUser);
            return "transfer";
        }

        // Perform transfer
        senderAccount.setBalance(senderAccount.getBalance() - amount);
        receiverAccount.setBalance(receiverAccount.getBalance() + amount);
        accountRepository.save(senderAccount);
        accountRepository.save(receiverAccount);

        // ================== Record transactions ==================

        // Debit transaction for sender
        Transaction debitTx = new Transaction();
        debitTx.setUser(loggedInUser); // sender
        debitTx.setAmount(amount);
        debitTx.setType("DEBIT");
        debitTx.setReceiverAccount(receiverAccount.getAccountNumber()); // show other party
        debitTx.setDateTime(LocalDateTime.now());
        transactionRepository.save(debitTx);

        // Credit transaction for receiver
        Transaction creditTx = new Transaction();
        creditTx.setUser(receiverAccount.getUser()); // receiver
        creditTx.setAmount(amount);
        creditTx.setType("CREDIT");
        creditTx.setReceiverAccount(senderAccount.getAccountNumber()); // show sender
        creditTx.setDateTime(LocalDateTime.now());
        transactionRepository.save(creditTx);

        model.addAttribute("message", "Transferred ₹" + amount + " to " + receiverAccountNumber);
        model.addAttribute("account", senderAccount);
        model.addAttribute("user", loggedInUser);

        return "transfer";
    }


    // ================== Transaction History ==================
    @GetMapping("/transactions")
    public String showTransactions(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        List<Transaction> transactions = transactionRepository.findByUser(loggedInUser);

        // Format date/time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
        transactions.forEach(tx -> tx.setFormattedDateTime(tx.getDateTime().format(formatter)));

        model.addAttribute("transactions", transactions);
        return "transactions";
    }

    // ================== Logout ==================
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
