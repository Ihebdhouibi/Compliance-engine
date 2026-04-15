package com.devteam.aiauditserver.services.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

@Service
public class MailServiceUtil {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendMail(String to, String subject, String content, boolean isHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // Use UTF-8 for everything
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Use the simpler setFrom version to avoid encoding issues
            helper.setFrom(fromEmail, "Ai Audit System");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, isHtml);
            helper.setReplyTo(fromEmail);

            mailSender.send(message);
            System.out.println("Email successfully sent to: " + to);
        } catch (Exception e) {
            // This will now print to your console even if @Async is on
            System.err.println("CRITICAL EMAIL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
