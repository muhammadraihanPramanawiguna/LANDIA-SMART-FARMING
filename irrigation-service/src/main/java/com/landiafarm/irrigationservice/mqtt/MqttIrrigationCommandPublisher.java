
package com.landiafarm.irrigationservice.mqtt;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MqttIrrigationCommandPublisher {

    private final MqttPahoMessageHandler mqttOutbound;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.topic}")
    private String topic;

    public MqttIrrigationCommandPublisher(
            MqttPahoMessageHandler mqttOutbound,
            ObjectMapper objectMapper) {

        this.mqttOutbound = mqttOutbound;
        this.objectMapper = objectMapper;
    }

    public void publish(
            String deviceId,
            String command) {

        try {
            String payload = objectMapper.writeValueAsString(
                    Map.of(
                            "deviceId", deviceId,
                            "command", command
                    )
            );

            Message<String> message =
                    MessageBuilder
                            .withPayload(payload)
                            .setHeader("mqtt_topic", topic)
                            .build();

            mqttOutbound.handleMessage(message);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Gagal mengirim perintah irigasi melalui MQTT",
                    e
            );
        }
    }
}
