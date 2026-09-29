package org.example.enrollmentservice.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentCreatedProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public EnrollmentCreatedProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${notification.kafka.enrollment-created-topic:enrollment-created}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendEnrollmentCreated(String studentEmail) {
        kafkaTemplate.send(topic, studentEmail, studentEmail);
    }
}
