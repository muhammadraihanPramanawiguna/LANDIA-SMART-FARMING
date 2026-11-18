
package com.landiafarm.waterlevelservice.mqtt;

import java.util.Map;

import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MqttWaterLevelAckPublisher {

    private final MqttPahoMessageHandler mqttAckOutbound;
    private final ObjectMapper objectMapper;

    public MqttWaterLevelAckPublisher(
            MqttPahoMessageHandler mqttAckOutbound,
            ObjectMapper objectMapper) {

        this.mqttAckOutbound = mqttAckOutbound;
        this.objectMapper = objectMapper;
    }

    public void publishStored(
            String recordId,
            String deviceId) throws Exception {

        String payload = objectMapper.writeValueAsString(
                Map.of(
                        "recordId", recordId,
                        "deviceId", deviceId,
                        "sensorType", "water-level",
                        "status", "stored"
                )
        );

        mqttAckOutbound.handleMessage(
                MessageBuilder.withPayload(payload).build()
        );
    }
}
