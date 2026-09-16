package com.landiafarm.dhtservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;

import com.landiafarm.dhtservice.mqtt.MqttDhtMessageListener;

@Configuration
public class MqttInboundConfig {

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.topic}")
    private String topic;

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInbound(
            MqttPahoClientFactory mqttClientFactory) {

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        clientId,
                        mqttClientFactory,
                        topic
                );

        adapter.setCompletionTimeout(5000);
        adapter.setQos(1);

        return adapter;
    }

    @Bean
    public IntegrationFlow mqttInboundFlow(
            MqttPahoMessageDrivenChannelAdapter adapter,
            MqttDhtMessageListener listener) {

        return IntegrationFlow
                .from(adapter)
                .handle(listener, "handleMessage")
                .get();
    }
}