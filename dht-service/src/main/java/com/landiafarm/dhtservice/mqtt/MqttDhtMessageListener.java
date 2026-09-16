package com.landiafarm.dhtservice.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.dhtservice.dto.DhtReadingRequest;
import com.landiafarm.dhtservice.service.DhtReadingService;

@Component
public class MqttDhtMessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    MqttDhtMessageListener.class
            );

    private final ObjectMapper objectMapper;
    private final DhtReadingService service;

    public MqttDhtMessageListener(
            ObjectMapper objectMapper,
            DhtReadingService service) {

        this.objectMapper = objectMapper;
        this.service = service;
    }

    public void handleMessage(Message<?> message) {

        try {

            String payload =
                    message.getPayload().toString();

            log.info(
                    "Received DHT MQTT message: {}",
                    payload
            );

            DhtReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            DhtReadingRequest.class
                    );

            service.save(request);

            log.info(
                    "DHT data successfully saved"
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process DHT MQTT message",
                    e
            );
        }
    }
}