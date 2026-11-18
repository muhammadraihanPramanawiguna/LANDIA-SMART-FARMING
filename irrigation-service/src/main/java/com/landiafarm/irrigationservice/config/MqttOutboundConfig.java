
package com.landiafarm.irrigationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;

@Configuration
public class MqttOutboundConfig {

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.topic}")
    private String topic;

    @Bean
    public MqttPahoMessageHandler mqttOutbound(
            MqttPahoClientFactory mqttClientFactory) {

        MqttPahoMessageHandler handler =
                new MqttPahoMessageHandler(
                        clientId + "-publisher",
                        mqttClientFactory
                );

        handler.setAsync(false);
        handler.setDefaultTopic(topic);
        handler.setDefaultQos(1);

        return handler;
    }
}
