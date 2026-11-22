package com.landiafarm.flowservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;

import com.landiafarm.flowservice.mqtt.MqttFlowMessageListener;

@Configuration
@EnableIntegration
public class MqttInboundConfig {

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.topic}")
    private String topic;

    @Bean
    public IntegrationFlow mqttFlowFlow(
            MqttPahoClientFactory mqttClientFactory,
            MqttFlowMessageListener listener) {

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        clientId,
                        mqttClientFactory,
                        topic);

        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(true);
        adapter.setConverter(converter);
        adapter.setCompletionTimeout(5000);
        adapter.setQos(1);

        // MQTT protocol ACK baru diselesaikan listener setelah DB dan Application ACK sukses.
        adapter.setManualAcks(true);

        return IntegrationFlow.from(adapter)
                .handle(listener, "handleMessage")
                .get();
    }
}
