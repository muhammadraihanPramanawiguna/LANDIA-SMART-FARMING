package com.landiafarm.waterlevelservice.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.waterlevelservice.dto.WaterLevelReadingRequest;
import com.landiafarm.waterlevelservice.service.WaterLevelReadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class MqttWaterLevelMessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    MqttWaterLevelMessageListener.class
            );

    private final ObjectMapper objectMapper;
    private final WaterLevelReadingService service;

    public MqttWaterLevelMessageListener(
            ObjectMapper objectMapper,
            WaterLevelReadingService service) {

        this.objectMapper = objectMapper;
        this.service = service;
    }

    public void handleMessage(Message<?> message) {

        try {

            String payload =
                    message.getPayload().toString();

            log.info(
                    "Received water level MQTT message: {}",
                    payload
            );

            WaterLevelReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            WaterLevelReadingRequest.class
                    );

            service.save(request);

            log.info(
                    "Water level data successfully saved"
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process water level MQTT message",
                    e
            );
        }
    }
}