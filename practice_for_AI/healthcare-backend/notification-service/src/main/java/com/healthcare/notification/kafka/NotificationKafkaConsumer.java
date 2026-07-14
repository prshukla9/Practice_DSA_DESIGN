package com.healthcare.notification.kafka;

import com.healthcare.common.events.DataFetchedEvent;
import com.healthcare.common.events.NotificationEvent;
import com.healthcare.common.kafka.KafkaTopics;
import com.healthcare.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaConsumer {

    private final NotificationService notificationService;

    public NotificationKafkaConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = KafkaTopics.DATA_FETCHED, groupId = "notification-service")
    public void onDataFetched(DataFetchedEvent event) {
        notificationService.notifyDataFetched(event);
    }

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_EVENTS, groupId = "notification-service")
    public void onNotificationEvent(NotificationEvent event) {
        notificationService.dispatch(event);
    }
}
