package com.landiafarm.flowservice.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.flowservice.dto.FlowReadingRequest;
import com.landiafarm.flowservice.service.FlowReadingService;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class MqttFlowMessageListener {

    private final ObjectMapper objectMapper;
    private final FlowReadingService flowReadingService;

    public MqttFlowMessageListener(
            ObjectMapper objectMapper,
            FlowReadingService flowReadingService) {

        this.objectMapper = objectMapper;
        this.flowReadingService = flowReadingService;
    }

    public void handleMessage(Message<?> message) {

        try {

            String payload =
                    message.getPayload().toString();

            System.out.println(
                    "MQTT Flow Message: " + payload
            );

            FlowReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            FlowReadingRequest.class
                    );

            flowReadingService.save(request);

            System.out.println(
                    "Flow reading berhasil disimpan."
            );

        } catch (Exception e) {

            System.err.println(
                    "Gagal memproses MQTT flow message: "
                            + e.getMessage()
            );
        }
    }
}