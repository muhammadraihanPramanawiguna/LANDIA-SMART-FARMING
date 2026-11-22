package com.landiafarm.irrigationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;

import com.landiafarm.irrigationservice.mqtt.MqttIrrigationEventListener;

@Configuration
@EnableIntegration
public class MqttInboundConfig {

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.event.topic}")
    private String eventTopic;

    @Bean
    public IntegrationFlow irrigationEventFlow(
            MqttPahoClientFactory mqttClientFactory,
            MqttIrrigationEventListener listener) {

        MqttPahoMessageDrivenChannelAdapter adapter =
            new MqttPahoMessageDrivenChannelAdapter(
                clientId + "-event-consumer",
                mqttClientFactory,
                eventTopic
            );

        DefaultPahoMessageConverter converter =
            new DefaultPahoMessageConverter();

        converter.setPayloadAsBytes(true);

        adapter.setConverter(converter);
        adapter.setCompletionTimeout(5000);
        adapter.setQos(1);

        // MQTT ACK diselesaikan setelah DB dan Application ACK berhasil.
        adapter.setManualAcks(true);

        return IntegrationFlow.from(adapter)
            .handle(listener, "handleMessage")
            .get();
    }
}