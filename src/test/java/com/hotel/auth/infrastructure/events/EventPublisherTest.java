package com.hotel.auth.infrastructure.events;

import com.hotel.auth.infrastructure.config.RabbitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    private static final String EXCHANGE = "hotel.events";

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventPublisher, "eventsExchange", EXCHANGE);
    }

    @Test
    void publishUserRegisteredSendsToRabbitTemplate() {
        UserRegisteredEvent event = new UserRegisteredEvent(1L, "name", "e@e.com", "USER");

        eventPublisher.publishUserRegistered(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(EXCHANGE),
                eq(RabbitConfig.USER_REGISTERED_ROUTING_KEY),
                eq(event)
        );
    }

    @Test
    void publishUserRegisteredSwallowsExceptions() {
        UserRegisteredEvent event = new UserRegisteredEvent(1L, "name", "e@e.com", "USER");
        doThrow(new AmqpException("connection failed"))
                .when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(RabbitConfig.USER_REGISTERED_ROUTING_KEY), eq(event));

        // No exception expected — el publisher loggea y sigue
        eventPublisher.publishUserRegistered(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq(EXCHANGE), eq(RabbitConfig.USER_REGISTERED_ROUTING_KEY), eq(event));
    }

    @Test
    void publishUserLoginSendsToRabbitTemplate() {
        UserLoginEvent event = new UserLoginEvent(2L, "user2", "u2@e.com", "USER");

        eventPublisher.publishUserLogin(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(EXCHANGE),
                eq(RabbitConfig.USER_LOGIN_ROUTING_KEY),
                eq(event)
        );
    }

    @Test
    void publishUserLoginSwallowsExceptions() {
        UserLoginEvent event = new UserLoginEvent(2L, "user2", "u2@e.com", "USER");
        doThrow(new AmqpException("connection failed"))
                .when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(RabbitConfig.USER_LOGIN_ROUTING_KEY), eq(event));

        eventPublisher.publishUserLogin(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq(EXCHANGE), eq(RabbitConfig.USER_LOGIN_ROUTING_KEY), eq(event));
    }

    @Test
    void publishPasswordResetSendsToRabbitTemplate() {
        PasswordResetEvent event = new PasswordResetEvent(3L, "user3", "u3@e.com", "123456");

        eventPublisher.publishPasswordReset(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(EXCHANGE),
                eq(RabbitConfig.USER_PASSWORD_RESET_ROUTING_KEY),
                eq(event)
        );
    }

    @Test
    void publishPasswordResetSwallowsExceptions() {
        PasswordResetEvent event = new PasswordResetEvent(3L, "user3", "u3@e.com", "123456");
        doThrow(new AmqpException("connection failed"))
                .when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(RabbitConfig.USER_PASSWORD_RESET_ROUTING_KEY), eq(event));

        eventPublisher.publishPasswordReset(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq(EXCHANGE), eq(RabbitConfig.USER_PASSWORD_RESET_ROUTING_KEY), eq(event));
    }
}
