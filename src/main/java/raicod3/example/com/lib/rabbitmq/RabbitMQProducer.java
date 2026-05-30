package raicod3.example.com.lib.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.dto.email.EmailRequest;

@Slf4j
@Component
public class RabbitMQProducer {
    private final RabbitTemplate rabbitTemplate;

    public RabbitMQProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendEmailNotification(EmailRequest req) {
        log.info("Dropping email request into queue for: {}", req.getEmail());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, req);
        log.info("Email request queued succesfully for: {}", req.getEmail());
    }
}
