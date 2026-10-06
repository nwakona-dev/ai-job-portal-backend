package com.emmzy.publisher;

import com.emmzy.config.RabbitMQConfig;
import com.emmzy.event.InterviewCancelledEvent;
import com.emmzy.event.InterviewRescheduledEvent;
import com.emmzy.event.InterviewScheduledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import com.emmzy.event.InterviewCompletedEvent;
import com.emmzy.event.InterviewMeetingLinkAddedEvent;
import com.emmzy.event.InterviewHiringDecisionEvent;

@Component
@RequiredArgsConstructor
public class InterviewEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishInterviewScheduled(InterviewScheduledEvent event) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.INTERVIEW_SCHEDULED_ROUTING_KEY,
                event
        );

        System.out.println(
                "Interview scheduled event published for interview: "
                        + event.getInterviewId()
        );
    }

    public void publishInterviewRescheduled(
            InterviewRescheduledEvent event
    ) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.INTERVIEW_RESCHEDULED_ROUTING_KEY,
                event
        );

        System.out.println(
                "Interview rescheduled event published for interview: "
                        + event.getInterviewId()
        );
    }

    public void publishInterviewCancelled(
            InterviewCancelledEvent event
    ) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.INTERVIEW_CANCELLED_ROUTING_KEY,
                event
        );

        System.out.println(
                "Interview cancelled event published for interview: "
                        + event.getInterviewId()
        );
    }

    public void publishInterviewCompleted(
            InterviewCompletedEvent event
    ) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.INTERVIEW_COMPLETED_ROUTING_KEY,
                event
        );

        System.out.println(
                "Interview completed event published for interview: "
                        + event.getInterviewId()
        );
    }

    public void publishInterviewMeetingLinkAdded(
            InterviewMeetingLinkAddedEvent event
    ) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.INTERVIEW_MEETING_LINK_ADDED_ROUTING_KEY,
                event
        );

        System.out.println(
                "Interview meeting link added event published for interview: "
                        + event.getInterviewId()
        );
    }

    public void publishInterviewHiringDecision(
            InterviewHiringDecisionEvent event
    ) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.INTERVIEW_HIRING_DECISION_ROUTING_KEY,
                event
        );

        System.out.println(
                "Interview hiring decision event published for interview: "
                        + event.getInterviewId()
        );
    }
}