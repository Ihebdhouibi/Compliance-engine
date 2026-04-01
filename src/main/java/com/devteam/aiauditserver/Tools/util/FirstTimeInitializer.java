package com.devteam.aiauditserver.Tools.util;


import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.services.auth.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class FirstTimeInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(FirstTimeInitializer.class);

    @Autowired
    private UserService userService;

    @Override
    public void run(String... string) throws Exception {
        initAdmin();
        initAuditor();
        initUser();
    }

    void initAdmin() {
        try {
            if (!userService.existByEmail("admin@gmail.com")) {
                logger.info("Database seeding: Creating admin...");

                User user = new User();
                user.setEmail("admin@gmail.com");
                user.setPassword("aze");
                user.setPhoneNumber("+21612345678");
                user.setUsername("AUDITADMIN");

                userService.saveAdmin(user);

                System.out.println("SEEDER SUCCESS: User 'admin@gmail.com' created.");
                logger.info("Admin created successfully.");
            } else {
                System.out.println("SEEDER SKIP: User 'admin@gmail.com' already exists.");
                logger.info("Admin already exists.");
            }
        } catch (Exception e) {
            System.err.println("SEEDER ERROR: Failed to seed user!");
            e.printStackTrace();
        }
    }


    void initAuditor() {
        try {
            if (!userService.existByEmail("auditor@gmail.com")) {
                logger.info("Database seeding: Creating auditor ..");

                User user = new User();
                user.setEmail("auditor@gmail.com");
                user.setPassword("aze");
                user.setPhoneNumber("+21612121212");
                user.setUsername("AUDITOR1");
                user.setFirstName("ch");
                user.setFirstName("Alex");

                userService.saveAuditor(user);

                System.out.println("SEEDER SUCCESS: Auditor 'auditor@gmail.com' created.");
                logger.info("Auditor created successfully.");
            } else {
                System.out.println("SEEDER SKIP: User 'auditor@gmail.com' already exists.");
                logger.info("Auditor already exists.");
            }
        } catch (Exception e) {
            System.err.println("SEEDER ERROR: Failed to seed user!");
            e.printStackTrace();
        }
    }

    void initUser() {
        try {
            if (!userService.existByEmail("user@gmail.com")) {
                logger.info("Database seeding: Creating user ..");

                User user = new User();
                user.setEmail("user@gmail.com");
                user.setPassword("aze");
                user.setPhoneNumber("+21612345698");
                user.setUsername("USER1");
                user.setFirstName("Ben Ali");
                user.setFirstName("Jhon");

                userService.saveUser(user);

                System.out.println("SEEDER SUCCESS: User 'user@gmail.com' created.");
                logger.info("user created successfully.");
            } else {
                System.out.println("SEEDER SKIP: User 'user@gmail.com' already exists.");
                logger.info("user already exists.");
            }
        } catch (Exception e) {
            System.err.println("SEEDER ERROR: Failed to seed user!");
            e.printStackTrace();
        }
    }
}