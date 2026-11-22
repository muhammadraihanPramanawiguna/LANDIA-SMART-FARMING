
package com.landiafarm.soilservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;

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

        // Terima payload MQTT sebagai byte[].
        // Listener akan mengubah byte tersebut menjadi UTF-8.
        DefaultPahoMessageConverter converter =
                new DefaultPahoMessageConverter();

        converter.setPayloadAsBytes(true);

adapter.setConverter(converter);
adapter.setCompletionTimeout(5000);
adapter.setQos(1);
adapter.setManualAcks(true);

        return IntegrationFlow
                .from(adapter)
                .handle(soilMessageListener, "handleMessage")
                .get();
    }
}
