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
import org.springframework.transaction.annotation.Transactional;
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
public String registerUser(@ModelAttribute User user, Model model) {
    try {
        // ✅ Username check
        if (userRepository.findByUsername(user.getUsername()) != null) {
            model.addAttribute("error", "Username already exists!");
            return "register";
        }

        // ✅ Email check
        if (userRepository.findByEmail(user.getEmail()) != null) {
            model.addAttribute("error", "Email already registered!");
            return "register";
        }

        user.setRole("USER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Don't set @Transient fields like active, createdAt
        
        User savedUser = userRepository.save(user);
        System.out.println("User saved with ID: " + savedUser.getId());

        // Create account automatically
        Account account = new Account();
        account.setUser(savedUser);
        account.setBalance(1000.0);
        account.setAccountNumber(generateAccountNumber());
        accountRepository.save(account);

        model.addAttribute("message",
                "Registration successful! Please login with your credentials.");
        return "login";
        
    } catch (Exception e) {
        System.out.println("Registration error: " + e.getMessage());
        e.printStackTrace();
        model.addAttribute("error", "Registration failed: " + e.getMessage());
        return "register";
    }
}
    // ================== Login ==================
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

@PostMapping("/login")
public String loginUser(@ModelAttribute("user") User user,
                        Model model,
                        HttpSession session) {
    try {
        System.out.println("Login attempt for username: " + user.getUsername());
        
        User existingUser = userRepository.findByUsername(user.getUsername());

        if (existingUser != null && 
            passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            
            System.out.println("Login successful for user: " + existingUser.getUsername());
            
            // Store user in session
            session.setAttribute("loggedInUser", existingUser);

            if ("ADMIN".equals(existingUser.getRole())) {
                return "redirect:/admin/dashboard";
            } else {
                // Get account for user - with null check
                Account account = accountRepository.findByUser(existingUser);
                
                // If account doesn't exist, create one
                if (account == null) {
                    System.out.println("No account found, creating new account");
                    account = new Account();
                    account.setUser(existingUser);
                    account.setBalance(1000.0);
                    account.setAccountNumber(generateAccountNumber());
                    account = accountRepository.save(account);
                }
                
                // Add attributes to model
                model.addAttribute("user", existingUser);
                model.addAttribute("account", account);
                
                return "dashboard";
            }
        } else {
            System.out.println("Login failed: invalid credentials");
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    } catch (Exception e) {
        System.out.println("ERROR in login: " + e.getMessage());
        e.printStackTrace();
        model.addAttribute("error", "System error: " + e.getMessage());
        return "login";
    }
}
    // ================== Dashboard ==================
    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) return "redirect:/login";

        Account account = accountRepository.findByUser(loggedInUser);
        model.addAttribute("user", loggedInUser);
        model.addAttribute("account", account);
        return "dashboard";
    }

    // ================== Transfer ==================
    @GetMapping("/transfer")
    public String showTransferPage(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) return "redirect:/login";

        Account account = accountRepository.findByUser(loggedInUser);
        model.addAttribute("user", loggedInUser);
        model.addAttribute("account", account);
        return "transfer";
    }

    @Transactional
    @PostMapping("/transfer")
    public String transferMoney(@RequestParam("receiverAccount") String receiverAccountNumber,
                                @RequestParam("amount") Double amount,
                                Model model,
                                HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) return "redirect:/login";

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

        senderAccount.setBalance(senderAccount.getBalance() - amount);
        receiverAccount.setBalance(receiverAccount.getBalance() + amount);

        accountRepository.save(senderAccount);
        accountRepository.save(receiverAccount);

        // Debit
        Transaction debitTx = new Transaction();
        debitTx.setUser(loggedInUser);
        debitTx.setAmount(amount);
        debitTx.setType("DEBIT");
        debitTx.setReceiverAccount(receiverAccount.getAccountNumber());
        debitTx.setDateTime(LocalDateTime.now());
        transactionRepository.save(debitTx);

        // Credit
        Transaction creditTx = new Transaction();
        creditTx.setUser(receiverAccount.getUser());
        creditTx.setAmount(amount);
        creditTx.setType("CREDIT");
        creditTx.setReceiverAccount(senderAccount.getAccountNumber());
        creditTx.setDateTime(LocalDateTime.now());
        transactionRepository.save(creditTx);

        model.addAttribute("message", "Transferred ₹" + amount + " to " + receiverAccountNumber);
        model.addAttribute("account", senderAccount);
        model.addAttribute("user", loggedInUser);

        return "transfer";
    }

    // ================== Transactions ==================
    @GetMapping("/transactions")
    public String showTransactions(Model model, HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) return "redirect:/login";

        List<Transaction> transactions = transactionRepository.findByUser(loggedInUser);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        double totalCredits = 0.0;
        double totalDebits = 0.0;

        if (transactions != null) {
            for (Transaction tx : transactions) {
                if (tx.getDateTime() != null) {
                    tx.setFormattedDateTime(tx.getDateTime().format(formatter));
                }

                if ("CREDIT".equals(tx.getType())) totalCredits += tx.getAmount();
                if ("DEBIT".equals(tx.getType())) totalDebits += tx.getAmount();
            }
        }

        model.addAttribute("transactions", transactions != null ? transactions : List.of());
        model.addAttribute("totalCredits", totalCredits);
        model.addAttribute("totalDebits", totalDebits);
        model.addAttribute("transactionCount", transactions != null ? transactions.size() : 0);

        return "transactions";
    }

    // ================== Logout ==================
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ================== Force Create Admin ==================
    @GetMapping("/force-create-admin")
    @ResponseBody
    public String forceCreateAdmin() {

        User admin = userRepository.findByUsername("admin");

        if (admin == null) {
            admin = new User();
            admin.setName("Administrator");
            admin.setEmail("admin@banking.com");
            admin.setUsername("admin");
            admin.setRole("ADMIN");
            // active is @Transient
            // admin.setActive(true);
        }

        admin.setPassword(passwordEncoder.encode("admin123"));

        // createdAt is @Transient
        // try {
        //     admin.setCreatedAt(LocalDateTime.now());
        // } catch (Exception ignored) {}

        userRepository.save(admin);

        return "<h2>✅ Admin ready</h2>" +
               "<p>Username: admin</p>" +
               "<p>Password: admin123</p>" +
               "<a href='/login'>Login</a>";
    }
}
