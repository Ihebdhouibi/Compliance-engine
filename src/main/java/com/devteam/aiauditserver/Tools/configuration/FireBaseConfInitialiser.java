package com.devteam.aiauditserver.Tools.configuration;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FireBaseConfInitialiser {
    Logger logger = LoggerFactory.getLogger(FireBaseConfInitialiser.class);

/*
    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        try (InputStream serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream()) {
            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(serviceAccount);
            FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                    .setCredentials(googleCredentials)
                    .build();

            FirebaseApp app;
            if (FirebaseApp.getApps().stream().noneMatch(a -> "Waynek".equals(a.getName()))) {
                app = FirebaseApp.initializeApp(firebaseOptions, "Waynek");
                logger.info("Firebase application 'Waynek' has been initialized");
            } else {
                app = FirebaseApp.getInstance("Waynek");
                logger.info("Firebase application 'Waynek' already initialized");
            }

            return FirebaseMessaging.getInstance(app);
        }
    }*/
}
