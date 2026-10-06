package com.emmzy.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

//    EXCHANGE
    public static final String EXCHANGE_NAME =
            "job.portal.exchange";

//    ROUTING KEYS
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

    public static final String INTERVIEW_HIRING_DECISION_ROUTING_KEY =
            "interview.hiring-decision";

//    QUEUE This queue will receive interview-related events that theNotification Service can consume.

    public static final String INTERVIEW_NOTIFICATION_QUEUE =
            "interview.notification.queue";

    @Bean
    public Queue interviewNotificationQueue() {
        return new Queue(
                INTERVIEW_NOTIFICATION_QUEUE,
                true
        );
    }

//    EXCHANGE

    @Bean
    public TopicExchange jobPortalExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }



//    MESSAGE CONVERTER Allows RabbitMQ messages to be sent/received as JSON

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

//    INTERVIEW EVENT BINDINGS
    @Bean
    public Binding interviewScheduledBinding(
            Queue interviewNotificationQueue,
            TopicExchange jobPortalExchange
    ) {
        return BindingBuilder
                .bind(interviewNotificationQueue)
                .to(jobPortalExchange)
                .with(INTERVIEW_SCHEDULED_ROUTING_KEY);
    }


    @Bean
    public Binding interviewRescheduledBinding(
            Queue interviewNotificationQueue,
            TopicExchange jobPortalExchange
    ) {
        return BindingBuilder
                .bind(interviewNotificationQueue)
                .to(jobPortalExchange)
                .with(INTERVIEW_RESCHEDULED_ROUTING_KEY);
    }


    @Bean
    public Binding interviewCancelledBinding(
            Queue interviewNotificationQueue,
            TopicExchange jobPortalExchange
    ) {
        return BindingBuilder
                .bind(interviewNotificationQueue)
                .to(jobPortalExchange)
                .with(INTERVIEW_CANCELLED_ROUTING_KEY);
    }


    @Bean
    public Binding interviewCompletedBinding(
            Queue interviewNotificationQueue,
            TopicExchange jobPortalExchange
    ) {
        return BindingBuilder
                .bind(interviewNotificationQueue)
                .to(jobPortalExchange)
                .with(INTERVIEW_COMPLETED_ROUTING_KEY);
    }


    @Bean
    public Binding interviewMeetingLinkAddedBinding(
            Queue interviewNotificationQueue,
            TopicExchange jobPortalExchange
    ) {
        return BindingBuilder
                .bind(interviewNotificationQueue)
                .to(jobPortalExchange)
                .with(INTERVIEW_MEETING_LINK_ADDED_ROUTING_KEY);
    }


    @Bean
    public Binding interviewHiringDecisionBinding(
            Queue interviewNotificationQueue,
            TopicExchange jobPortalExchange
    ) {
        return BindingBuilder
                .bind(interviewNotificationQueue)
                .to(jobPortalExchange)
                .with(INTERVIEW_HIRING_DECISION_ROUTING_KEY);
    }
}

