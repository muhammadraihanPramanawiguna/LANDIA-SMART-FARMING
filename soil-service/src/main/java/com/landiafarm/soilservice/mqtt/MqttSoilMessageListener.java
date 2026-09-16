package com.landiafarm.soilservice.mqtt;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.soilservice.dto.SoilReadingRequest;
import com.landiafarm.soilservice.service.SoilReadingService;

@Component
public class MqttSoilMessageListener {

    private final SoilReadingService soilReadingService;
    private final ObjectMapper objectMapper;

    public MqttSoilMessageListener(
            SoilReadingService soilReadingService,
            ObjectMapper objectMapper) {

        this.soilReadingService = soilReadingService;
        this.objectMapper = objectMapper;
    }

    public void handleMessage(Message<?> message) {

        try {

            String payload = message.getPayload().toString();

            System.out.println(
                    "MQTT Soil Message: " + payload
            );

            SoilReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            SoilReadingRequest.class
                    );

            soilReadingService.saveReading(request);

            System.out.println(
                    "Soil reading berhasil disimpan."
            );

        } catch (Exception e) {

            System.err.println(
                    "Gagal memproses MQTT soil message:"
            );

            e.printStackTrace();
        }
    }
}