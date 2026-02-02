package com.hotel.auth.infrastructure.events;

import com.hotel.auth.infrastructure.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EVENTS_EXCHANGE,
                    RabbitConfig.USER_REGISTERED_ROUTING_KEY,
                    event
            );
            LOGGER.info("[EVENT] UserRegistered published for userId: {}", event.getUserId());
        } catch (Exception e) {
            LOGGER.error("[EVENT] Error publishing UserRegistered event: {}", e.getMessage(), e);
        }
    }

    public void publishUserLogin(UserLoginEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EVENTS_EXCHANGE,
                    RabbitConfig.USER_LOGIN_ROUTING_KEY,
                    event
            );
            LOGGER.info("[EVENT] UserLogin published for userId: {}", event.getUserId());
        } catch (Exception e) {
            LOGGER.error("[EVENT] Error publishing UserLogin event: {}", e.getMessage(), e);
        }
    }
}
