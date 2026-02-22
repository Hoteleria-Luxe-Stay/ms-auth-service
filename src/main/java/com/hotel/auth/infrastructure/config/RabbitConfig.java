package com.hotel.auth.infrastructure.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Routing keys: identificadores de protocolo fijos
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String USER_LOGIN_ROUTING_KEY = "user.login";
    public static final String USER_PASSWORD_RESET_ROUTING_KEY = "user.password.reset";

    // Exchange name: configurable desde config-server
    @Value("${app.rabbitmq.sesion.exchange:hotel.events}")
    private String eventsExchangeName;

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(eventsExchangeName);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
