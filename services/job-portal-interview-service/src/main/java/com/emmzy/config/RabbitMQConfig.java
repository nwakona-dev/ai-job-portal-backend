package com.emmzy.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME =
            "job.portal.exchange";

    public static final String INTERVIEW_SCHEDULED_ROUTING_KEY =
            "interview.scheduled";

    public static final String INTERVIEW_RESCHEDULED_ROUTING_KEY =
            "interview.rescheduled";

    public static final String INTERVIEW_CANCELLED_ROUTING_KEY =
            "interview.cancelled";

    public static final String INTERVIEW_COMPLETED_ROUTING_KEY =
            "interview.completed";

    public static final String INTERVIEW_MEETING_LINK_ADDED_ROUTING_KEY =
            "interview.meeting-link-added";

    @Bean
    public TopicExchange jobPortalExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}