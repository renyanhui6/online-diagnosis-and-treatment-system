package cn.edu.ncu.medical.registration;

import cn.edu.ncu.medical.entity.dto.AppointmentReservationMessage;
import cn.edu.ncu.medical.service.RegistrationService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.registration.reservation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AppointmentReservationConsumer {
    private static final String RETRY_HEADER = "x-appointment-retry";

    private final RegistrationService registrationService;
    private final RabbitTemplate rabbitTemplate;
    private final AppointmentReservationProperties properties;

    public AppointmentReservationConsumer(RegistrationService registrationService,
                                          RabbitTemplate rabbitTemplate,
                                          AppointmentReservationProperties properties) {
        this.registrationService = registrationService;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @RabbitListener(
            queues = AppointmentReservationRabbitConfig.APPOINTMENT_CREATE_QUEUE,
            containerFactory = "appointmentReservationListenerContainerFactory"
    )
    public void consume(AppointmentReservationMessage payload, Message rawMessage, Channel channel) throws IOException {
        long tag = rawMessage.getMessageProperties().getDeliveryTag();
        int retryCount = resolveRetryCount(rawMessage);
        try {
            registrationService.consumeReservedRegistration(payload);
            channel.basicAck(tag, false);
        } catch (RetryableAppointmentReservationException ex) {
            if (retryCount >= properties.getConsumerMaxRetries()) {
                log.warn("Appointment reservation retry exhausted, token={}, retries={}", payload.getToken(), retryCount, ex);
                safelyFailReservation(payload, "挂号处理重试超限");
                channel.basicAck(tag, false);
                return;
            }
            try {
                rabbitTemplate.convertAndSend(
                        AppointmentReservationRabbitConfig.APPOINTMENT_EXCHANGE,
                        AppointmentReservationRabbitConfig.APPOINTMENT_ROUTING_KEY,
                        payload,
                        message -> {
                            message.getMessageProperties().setHeader(RETRY_HEADER, retryCount + 1);
                            return message;
                        }
                );
                channel.basicAck(tag, false);
            } catch (RuntimeException publishEx) {
                log.warn("Republish appointment reservation failed, token={}", payload.getToken(), publishEx);
                channel.basicNack(tag, false, true);
            }
        } catch (Exception ex) {
            log.error("Consume appointment reservation failed, token={}", payload.getToken(), ex);
            safelyFailReservation(payload, "挂号处理失败");
            channel.basicAck(tag, false);
        }
    }

    private int resolveRetryCount(Message rawMessage) {
        Object header = rawMessage.getMessageProperties().getHeaders().get(RETRY_HEADER);
        if (header instanceof Number number) {
            return number.intValue();
        }
        if (header instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private void safelyFailReservation(AppointmentReservationMessage payload, String reason) {
        try {
            registrationService.failReservation(payload, reason);
        } catch (Exception ex) {
            log.warn("Fail reservation rollback failed, token={}", payload.getToken(), ex);
        }
    }
}
