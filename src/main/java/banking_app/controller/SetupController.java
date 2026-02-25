package banking_app.controller;

import banking_app.entity.User;
import banking_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.time.LocalDateTime;

@Controller
public class SetupController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/setup")
    @ResponseBody
    public String setup() {
        StringBuilder result = new StringBuilder();
        result.append("<html><body style='font-family: Arial; padding: 20px;'>");
        
        try {
            // Check if any users exist
            long userCount = userRepository.count();
            result.append("<h2>🔧 System Setup</h2>");
            result.append("<p>Total users in database: ").append(userCount).append("</p>");
            
            if (userCount == 0) {
                // Create admin user
                User admin = new User();
                admin.setName("Administrator");
                admin.setEmail("admin@banking.com");
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("ADMIN");
                admin.setActive(true);
                admin.setCreatedAt(LocalDateTime.now());
                
                userRepository.save(admin);
                
                result.append("<div style='background-color: #d4edda; color: #155724; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
                result.append("<h3>✅ Admin User Created Successfully!</h3>");
                result.append("<p><strong>Username:</strong> admin</p>");
                result.append("<p><strong>Password:</strong> admin123</p>");
                result.append("</div>");
            } else {
                // Check if admin exists
                boolean adminExists = userRepository.findAll()
                    .stream()
                    .anyMatch(u -> "ADMIN".equals(u.getRole()));
                
                if (!adminExists) {
                    // Promote first user to admin
                    User firstUser = userRepository.findAll().get(0);
                    firstUser.setRole("ADMIN");
                    userRepository.save(firstUser);
                    
                    result.append("<div style='background-color: #fff3cd; color: #856404; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
                    result.append("<h3>⚠️ Promoted User to Admin</h3>");
                    result.append("<p><strong>Username:</strong> ").append(firstUser.getUsername()).append("</p>");
                    result.append("<p>This user is now an admin. Use their credentials to login.</p>");
                    result.append("</div>");
                }
            }
            
            // Display all users
            result.append("<h3>Current Users:</h3>");
            result.append("<table border='1' cellpadding='8' style='border-collapse: collapse;'>");
            result.append("<tr><th>ID</th><th>Username</th><th>Name</th><th>Email</th><th>Role</th><th>Active</th></tr>");
            
            for (User user : userRepository.findAll()) {
                result.append("<tr>")
                    .append("<td>").append(user.getId()).append("</td>")
                    .append("<td>").append(user.getUsername()).append("</td>")
                    .append("<td>").append(user.getName()).append("</td>")
                    .append("<td>").append(user.getEmail()).append("</td>")
                    .append("<td><strong>").append(user.getRole()).append("</strong></td>")
                    .append("<td>").append(user.isActive() ? "✅" : "❌").append("</td>")
                    .append("</tr>");
            }
            result.append("</table>");
            
            result.append("<p style='margin-top: 30px;'>");
            result.append("<a href='/login' style='background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Go to Login Page</a>");
            result.append("</p>");
            
        } catch (Exception e) {
            result.append("<div style='background-color: #f8d7da; color: #721c24; padding: 15px; border-radius: 5px;'>");
            result.append("<h3>❌ Error:</h3>");
            result.append("<p>").append(e.getMessage()).append("</p>");
            result.append("</div>");
        }
        
        result.append("</body></html>");
        return result.toString();
    }
}