package com.landiafarm.waterlevelservice.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {

        MqttConnectOptions options =
                new MqttConnectOptions();

        options.setServerURIs(
                new String[]{brokerUrl}
        );

        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);

        DefaultMqttPahoClientFactory factory =
                new DefaultMqttPahoClientFactory();

        factory.setConnectionOptions(options);

        return factory;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}