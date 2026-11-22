package com.landiafarm.irrigationservice.mqtt;

import java.util.Map;

import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MqttIrrigationAckPublisher {

    private final MqttPahoMessageHandler mqttAckOutbound;
    private final ObjectMapper objectMapper;

    public MqttIrrigationAckPublisher(
            MqttPahoMessageHandler mqttAckOutbound,
            ObjectMapper objectMapper) {

        this.mqttAckOutbound = mqttAckOutbound;
        this.objectMapper = objectMapper;
    }

    public void publishStored(
            String recordId,
            String deviceId) throws Exception {

        if (recordId == null || recordId.isBlank()) {
            throw new IllegalArgumentException(
                "recordId ACK tidak boleh kosong"
            );
        }

        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException(
                "deviceId ACK tidak boleh kosong"
            );
        }

        String payload = objectMapper.writeValueAsString(
            Map.of(
                "recordId", recordId,
                "deviceId", deviceId,
                "sensorType", "irrigation",
                "status", "stored"
            )
        );

        mqttAckOutbound.handleMessage(
            MessageBuilder.withPayload(payload).build()
        );
    }
}