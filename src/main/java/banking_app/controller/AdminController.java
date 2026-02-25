package banking_app.controller;

import banking_app.entity.*;
import banking_app.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;

    // Admin authentication check
    private boolean isAdmin(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        return loggedInUser != null && "ADMIN".equals(loggedInUser.getRole());
    }

    // ================== Admin Dashboard Home ==================
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        // Statistics for dashboard
        long totalUsers = userRepository.count();
        long totalAccounts = accountRepository.count();
        long totalTransactions = transactionRepository.count();
        
        // Calculate total balance across all accounts
        List<Account> allAccounts = accountRepository.findAll();
        Double totalBalance = allAccounts.stream()
                .mapToDouble(Account::getBalance)
                .sum();
        
        // Get recent transactions
        List<Transaction> recentTransactions = transactionRepository.findTop10ByOrderByDateTimeDesc();

        // Format dates for display
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (recentTransactions != null) {
            recentTransactions.forEach(tx -> {
                if (tx.getDateTime() != null) {
                    tx.setFormattedDateTime(tx.getDateTime().format(formatter));
                }
            });
        }

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalAccounts", totalAccounts);
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("totalBalance", totalBalance != null ? totalBalance : 0.0);
        model.addAttribute("recentTransactions", recentTransactions != null ? recentTransactions : List.of());

        return "admin/dashboard";  // Direct page return
    }

    // ================== User Management ==================
    @GetMapping("/users")
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model,
            HttpSession session) {
        
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> userPage;
        
        if (search != null && !search.isEmpty()) {
            userPage = userRepository.findByUsernameContainingOrEmailContainingOrNameContaining(
                    search, search, search, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        // Get account info for each user
        Map<Long, Account> userAccounts = new HashMap<>();
        if (userPage.hasContent()) {
            for (User user : userPage.getContent()) {
                try {
                    Account account = accountRepository.findByUser(user);
                    if (account != null) {
                        userAccounts.put(user.getId(), account);
                    }
                } catch (Exception e) {
                    // Log error but continue
                }
            }
        }

        model.addAttribute("userPage", userPage);
        model.addAttribute("userAccounts", userAccounts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("search", search);

        return "admin/users";  // Direct page return
    }

    @GetMapping("/user/{id}")
    public String viewUserDetails(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return "redirect:/admin/users";
        }

        Account account = accountRepository.findByUser(user);
        List<Transaction> transactions = transactionRepository.findByUser(user);

        // Format dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (transactions != null) {
            transactions.forEach(tx -> {
                if (tx.getDateTime() != null) {
                    tx.setFormattedDateTime(tx.getDateTime().format(formatter));
                }
            });
        }

        model.addAttribute("user", user);
        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions != null ? transactions : List.of());

        return "admin/user-details";  // Direct page return
    }

    @PostMapping("/user/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, 
                                   RedirectAttributes redirectAttributes,
                                   HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setActive(!user.isActive());
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("message", 
                "User " + user.getUsername() + " has been " + 
                (user.isActive() ? "activated" : "deactivated"));
        }

        return "redirect:/admin/users";
    }

    // ================== Transaction Monitoring ==================
    @GetMapping("/transactions")
    public String listAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String username,
            Model model,
            HttpSession session) {
        
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("dateTime").descending());
        Page<Transaction> transactionPage;
        
        if (type != null && !type.isEmpty()) {
            transactionPage = transactionRepository.findByType(type, pageable);
        } else if (username != null && !username.isEmpty()) {
            User user = userRepository.findByUsername(username);
            if (user != null) {
                transactionPage = transactionRepository.findByUser(user, pageable);
            } else {
                transactionPage = Page.empty();
            }
        } else {
            transactionPage = transactionRepository.findAll(pageable);
        }

        // Format dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (transactionPage.hasContent()) {
            transactionPage.getContent().forEach(tx -> {
                if (tx.getDateTime() != null) {
                    tx.setFormattedDateTime(tx.getDateTime().format(formatter));
                }
            });
        }

        model.addAttribute("transactionPage", transactionPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionPage.getTotalPages());
        model.addAttribute("type", type);
        model.addAttribute("username", username);

        return "admin/transactions";  // Direct page return
    }

    // ================== System Statistics ==================
    @GetMapping("/statistics")
    public String showStatistics(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        // Daily statistics for the last 7 days
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        
        List<Object[]> dailyTransactions = transactionRepository.getDailyTransactionCount(weekAgo);
        List<Object[]> transactionVolume = transactionRepository.getDailyTransactionVolume(weekAgo);
        List<Object[]> userRegistrations = userRepository.getDailyRegistrations(weekAgo);

        model.addAttribute("dailyTransactions", dailyTransactions != null ? dailyTransactions : List.of());
        model.addAttribute("transactionVolume", transactionVolume != null ? transactionVolume : List.of());
        model.addAttribute("userRegistrations", userRegistrations != null ? userRegistrations : List.of());

        return "admin/statistics";  // Direct page return
    }

    // ================== Create Admin User ==================
    @GetMapping("/create-admin")
    public String showCreateAdminForm(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", new User());

        return "admin/create-admin";  // Direct page return
    }

    @PostMapping("/create-admin")
    public String createAdmin(@ModelAttribute User user, 
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (userRepository.findByUsername(user.getUsername()) != null) {
            redirectAttributes.addFlashAttribute("error", "Username already exists");
            return "redirect:/admin/create-admin";
        }

        user.setRole("ADMIN");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("message", "Admin user created successfully");
        return "redirect:/admin/users";
    }
}