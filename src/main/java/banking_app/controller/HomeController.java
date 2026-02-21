package banking_app.controller;

import banking_app.model.User; // Your User class
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("user", new User()); // Add empty user object for Thymeleaf
        return "register";   // matches register.html in templates
    }
}
