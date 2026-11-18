
package com.landiafarm.rainservice.mqtt;

import java.util.Map;

import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MqttRainAckPublisher {

    private final MqttPahoMessageHandler mqttAckOutbound;
    private final ObjectMapper objectMapper;

    public MqttRainAckPublisher(
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
                        "sensorType", "rain",
                        "status", "stored"
                )
        );

        mqttAckOutbound.handleMessage(
                MessageBuilder.withPayload(payload).build()
        );
    }
}
