
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

    public void publishStored(String recordId) throws Exception {

        String payload = objectMapper.writeValueAsString(
                Map.of(
                        "recordId", recordId,
                        "status", "stored"
                )
        );

        mqttAckOutbound.handleMessage(
                MessageBuilder.withPayload(payload).build()
        );
    }
}
