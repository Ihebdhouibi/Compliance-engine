package com.devteam.aiauditserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiAuditServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAuditServerApplication.class, args);
    }

}
