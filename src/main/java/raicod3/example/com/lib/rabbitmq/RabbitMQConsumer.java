package raicod3.example.com.lib.rabbitmq;

import com.rabbitmq.client.Channel;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.dto.email.EmailRequest;
import raicod3.example.com.service.NotificationService;

@Slf4j
@Component
public class RabbitMQConsumer {

    private final NotificationService notificationService;

    public RabbitMQConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE, ackMode = "MANUAL")
    public void consumeEmailNotification(EmailRequest emailRequest, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.debug("Picked up email request from queue for: {}", emailRequest.getEmail());
        try {
            notificationService.sendEmail(
                    emailRequest,
                    emailRequest.getOtpToken(),
                    emailRequest.getTemplatePath()
            );

            // tell rabbitmq: message processed successfully, remove it from queue
            channel.basicAck(deliveryTag, false);
            log.info("Email sent and acknowledged for: {}", emailRequest.getEmail());
        } catch(MessagingException e) {
            log.error("Failed to send email to: {}. Reason: {}", emailRequest.getEmail(), e.getMessage());
            try {
                // tell rabbitmq: this failed, do not requeue it - send to DLQ instead
                channel.basicNack(deliveryTag, false, false);
                log.warn("Email message sent to DQL: {}", emailRequest.getEmail());
            } catch (Exception nackEx) {
                log.error("Failed to nack message: {}", nackEx.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error processing email for: {}. Reason: {}", emailRequest.getEmail(), e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, false);
                log.warn("Email message sent to DLQ for: {}", emailRequest.getEmail());
            } catch (Exception nackEx) {
                log.error("Failed to nack message: {}", nackEx.getMessage());
            }
        }
    }
}
