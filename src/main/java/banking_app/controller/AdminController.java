package banking_app.controller;

import banking_app.entity.User;
import banking_app.entity.Transaction;
import banking_app.repository.UserRepository;
import banking_app.repository.TransactionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // 🔐 Check admin session - Fixed to check User object from session
    private boolean isAdmin(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        return loggedInUser != null && "ADMIN".equals(loggedInUser.getRole());
    }

    // ===============================
    // 📊 Admin Dashboard
    // ===============================
  @GetMapping("/dashboard")
public String adminDashboard(Model model, HttpSession session) {
    if (!isAdmin(session)) {
        return "redirect:/login";
    }

    long totalUsers = userRepository.count();
    long totalAccounts = accountRepository.count();
    long totalTransactions = transactionRepository.count();
    
    // Calculate total balance
    List<Account> allAccounts = accountRepository.findAll();
    double totalBalance = allAccounts.stream()
        .mapToDouble(Account::getBalance)
        .sum();
    
    // Get recent transactions (last 10)
    List<Transaction> recentTransactions = transactionRepository.findTop10ByOrderByDateTimeDesc();
    
    // Format dates
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    recentTransactions.forEach(tx -> {
        if (tx.getDateTime() != null) {
            tx.setFormattedDateTime(tx.getDateTime().format(formatter));
        }
    });

    model.addAttribute("totalUsers", totalUsers);
    model.addAttribute("totalAccounts", totalAccounts);
    model.addAttribute("totalBalance", totalBalance);
    model.addAttribute("totalTransactions", totalTransactions);
    model.addAttribute("recentTransactions", recentTransactions);

    return "admin/dashboard";
}
    // ===============================
    // 👥 View Users (with pagination & search)
    // ===============================
    @GetMapping("/users")
    public String viewUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Page<User> userPage;

        if (keyword != null && !keyword.isEmpty()) {
            userPage = userRepository
                    .findByUsernameContainingOrEmailContainingOrNameContaining(
                            keyword, keyword, keyword, PageRequest.of(page, 10));
        } else {
            userPage = userRepository.findAll(PageRequest.of(page, 10));
        }

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "admin/users";
    }

    // ===============================
    // 🔄 Toggle User Active/Inactive
    // ===============================
    @GetMapping("/toggle-user/{id}")
    public String toggleUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        User user = userRepository.findById(id).orElse(null);

        if (user != null) {
            // Since active is @Transient, we need to handle this differently
            // Option 1: You might want to add an 'active' column to database
            // Option 2: For now, just redirect back without changing
            // user.setActive(!user.isActive());
            // userRepository.save(user);
        }

        return "redirect:/admin/users";
    }

    // ===============================
    // 💳 View All Transactions
    // ===============================
    @GetMapping("/transactions")
    public String viewTransactions(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<Transaction> transactions = transactionRepository.findAll();
        model.addAttribute("transactions", transactions);

        return "admin/transactions";
    }

    // ===============================
    // 📈 Statistics (Transactions Only)
    // ===============================
    @GetMapping("/statistics")
    public String showStatistics(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        List<Object[]> dailyTransactions =
                transactionRepository.getDailyTransactionCount(weekAgo);

        List<Object[]> transactionVolume =
                transactionRepository.getDailyTransactionVolume(weekAgo);

        model.addAttribute("dailyTransactions",
                dailyTransactions != null ? dailyTransactions : List.of());

        model.addAttribute("transactionVolume",
                transactionVolume != null ? transactionVolume : List.of());

        return "admin/statistics";
    }

    // ===============================
    // ➕ Create Admin
    // ===============================
    @PostMapping("/create-admin")
    public String createAdmin(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (userRepository.findByUsername(username) != null) {
            return "redirect:/admin/users?error=exists";
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password); // 🔒 In production use BCrypt
        user.setRole("ADMIN");
        // active is @Transient, so it won't be saved
        // user.setActive(true);

        userRepository.save(user);

        return "redirect:/admin/users?success=created";
    }
}
