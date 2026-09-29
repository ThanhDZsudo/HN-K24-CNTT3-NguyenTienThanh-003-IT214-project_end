package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EnrollmentCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentCreatedConsumer.class);

    private final EmailService emailService;

    public EnrollmentCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "${notification.kafka.enrollment-created-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String email) {
        String trimmedEmail = email == null ? "" : email.trim();
        if (!StringUtils.hasText(trimmedEmail)) {
            log.warn("Received blank enrollment email from Kafka topic");
            return;
        }

        emailService.sendEnrollmentCreatedEmail(trimmedEmail);
    }
}
