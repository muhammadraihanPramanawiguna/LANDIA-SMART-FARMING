package com.landiafarm.soilservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;

import com.landiafarm.soilservice.mqtt.MqttSoilMessageListener;

@Configuration
@EnableIntegration
public class MqttInboundConfig {

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.topic}")
    private String topic;

    @Bean
    public IntegrationFlow mqttSoilFlow(
            DefaultMqttPahoClientFactory mqttClientFactory,
            MqttSoilMessageListener soilMessageListener) {

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        clientId,
                        mqttClientFactory,
                        topic
                );

        adapter.setCompletionTimeout(5000);
        adapter.setQos(1);

        return IntegrationFlow
                .from(adapter)
                .handle(soilMessageListener, "handleMessage")
                .get();
    }
}