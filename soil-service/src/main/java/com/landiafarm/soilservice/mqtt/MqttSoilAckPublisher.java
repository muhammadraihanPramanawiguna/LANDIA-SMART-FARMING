
package com.landiafarm.soilservice.mqtt;

import java.util.Map;

import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MqttSoilAckPublisher {

    private final MqttPahoMessageHandler mqttAckOutbound;
    private final ObjectMapper objectMapper;

    public MqttSoilAckPublisher(
            MqttPahoMessageHandler mqttAckOutbound,
            ObjectMapper objectMapper) {

        this.mqttAckOutbound = mqttAckOutbound;
        this.objectMapper = objectMapper;
    }

    public void publishStored(
            String recordId,
            String deviceId
    ) throws Exception {

        if (recordId == null || recordId.isBlank()) {
            throw new IllegalArgumentException(
                    "recordId tidak boleh kosong"
            );
        }

        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException(
                    "deviceId tidak boleh kosong"
            );
        }

        String payload = objectMapper.writeValueAsString(
                Map.of(
                        "recordId", recordId,
                        "deviceId", deviceId,
                        "sensorType", "soil",
                        "status", "stored"
                )
        );

        mqttAckOutbound.handleMessage(
                MessageBuilder
                        .withPayload(payload)
                        .build()
        );
    }
}
