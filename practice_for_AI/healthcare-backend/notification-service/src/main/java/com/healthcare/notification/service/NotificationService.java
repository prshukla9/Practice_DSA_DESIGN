package com.healthcare.notification.service;

import com.healthcare.common.events.DataFetchedEvent;
import com.healthcare.common.events.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyDataFetched(DataFetchedEvent event) {
        NotificationEvent notification = new NotificationEvent(
                UUID.randomUUID().toString(),
                event.getPatientId(),
                "SMS",
                "Your health records (" + event.getResourceType() + ") were fetched for consented care workflow.",
                "DATA_FETCHED",
                Instant.now()
        );
        dispatch(notification);
    }

    public void dispatch(NotificationEvent event) {
        // Production: integrate SNS/SES/Twilio
        log.info("NOTIFICATION channel={} patient={} message={}",
                event.getChannel(), event.getPatientId(), event.getMessage());
    }
}
