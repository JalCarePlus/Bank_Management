
# Banking Application - Spring Boot

A **full-stack Banking Web Application** built using **Java, Spring Boot, Thymeleaf, MySQL**, and **Bootstrap**.  
Users can **register, login, transfer money, view transaction history**, and manage their accounts.  

---

## 🔹 Features

- **User Registration & Login**
  - Secure registration and login
  - Passwords stored securely (BCrypt can be integrated)
  
- **Dashboard**
  - Displays account info: name, account number, balance
  - Quick access to transfer money, transaction history, and logout

- **Transfer Money**
  - Users can transfer money to other accounts
  - Validations for sufficient balance and account existence

- **Transaction History**
  - View all debit/credit transactions
  - Formatted date and time (`dd/MM/yyyy hh:mm a`)
  - Shows sender/receiver details

- **Logout**
  - Ends user session securely

---

## 🔹 Technologies Used

- **Backend:** Java, Spring Boot, Spring Data JPA, Hibernate  
- **Frontend:** Thymeleaf, Bootstrap 5  
- **Database:** H2 (in-memory) or MySQL  
- **Version Control:** Git & GitHub  
- **Build Tool:** Maven  

---

## 🔹 Project Structure

```

banking-app/
│
├─ src/main/java/banking_app/
│   ├─ controller/       # Handles web requests
│   ├─ entity/           # JPA entities (User, Account, Transaction)
│   ├─ repository/       # Spring Data repositories
│   ├─ config/           # Security / Password configs
│   └─ BankingAppApplication.java
│
├─ src/main/resources/
│   ├─ templates/        # Thymeleaf HTML pages
│   └─ application.properties
│
├─ pom.xml
└─ .gitignore

````

---

## 🔹 How to Run

1. **Clone the repository**
```bash
git clone https://github.com/RajuNadapana/banking-application-springboot.git
cd banking-application-springboot
````

2. **Build the project**

```bash
mvn clean install
```

3. **Run the Spring Boot application**

```bash
mvn spring-boot:run
```

4. **Access the application**

```
http://localhost:9090/
```

5. **Default workflow**

```
Register → Login → Dashboard → Transfer / View Transactions → Logout
```

---

## 🔹 Notes

* Dates and times formatted as `dd/MM/yyyy hh:mm a`
* Validations for empty fields, incorrect login, and insufficient balance
* Reset the database by deleting all records in H2/MySQL tables

---

## 🔹 Future Enhancements

* Integrate **Spring Security** for encrypted passwords
* Add **REST API endpoints** for mobile apps
* Add **transaction filters** (by date, amount, type)
* Add **admin panel** for user and account management

---

## 🔹 Author

**SubbaRaju Nadapana**

* Email: [nadapanaraju8999@gmail.com](mailto:nadapanaraju8999@gmail.com)
* GitHub: [[RajuNadapana]](https://github.com/RajuNadapana)
* LinkedIn: [[Your LinkedIn URL](https://www.linkedin.com/in/raju-nadapana)]



