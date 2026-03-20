package cn.edu.ncu.medical.registration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppointmentReservationProperties.class)
public class AppointmentReservationRabbitConfig {
    public static final String APPOINTMENT_EXCHANGE = "appointment.registration.exchange";
    public static final String APPOINTMENT_CREATE_QUEUE = "appointment.registration.create.queue";
    public static final String APPOINTMENT_ROUTING_KEY = "appointment.registration.create";

    @Bean
    public DirectExchange appointmentRegistrationExchange() {
        return new DirectExchange(APPOINTMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue appointmentRegistrationQueue() {
        return QueueBuilder.durable(APPOINTMENT_CREATE_QUEUE).build();
    }

    @Bean
    public Binding appointmentRegistrationBinding(Queue appointmentRegistrationQueue,
                                                  DirectExchange appointmentRegistrationExchange) {
        return BindingBuilder.bind(appointmentRegistrationQueue)
                .to(appointmentRegistrationExchange)
                .with(APPOINTMENT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter appointmentRabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory appointmentReservationListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter appointmentRabbitMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(appointmentRabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        return factory;
    }
}
