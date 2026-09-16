package com.landiafarm.irrigationservice.mqtt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class MqttIrrigationCommandPublisher {

    private final MqttPahoMessageHandler mqttOutbound;

    @Value("${mqtt.topic}")
    private String topic;

    public MqttIrrigationCommandPublisher(
            MqttPahoMessageHandler mqttOutbound) {

        this.mqttOutbound = mqttOutbound;
    }

    public void publish(
            String deviceId,
            String command) {

        String payload =
                "{"
                        + "\"deviceId\":\"" + deviceId + "\","
                        + "\"command\":\"" + command + "\""
                        + "}";

        Message<String> message =
                MessageBuilder
                        .withPayload(payload)
                        .setHeader(
                                "mqtt_topic",
                                topic
                        )
                        .build();

        mqttOutbound.handleMessage(message);
    }
}