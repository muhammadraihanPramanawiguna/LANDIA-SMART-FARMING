package com.landiafarm.rainservice.mqtt;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.rainservice.dto.RainReadingRequest;
import com.landiafarm.rainservice.service.RainReadingService;

@Component
public class MqttRainMessageListener {

    private final ObjectMapper objectMapper;
    private final RainReadingService rainReadingService;

    public MqttRainMessageListener(
            ObjectMapper objectMapper,
            RainReadingService rainReadingService) {

        this.objectMapper = objectMapper;
        this.rainReadingService = rainReadingService;
    }

    public void handleMessage(Message<?> message) {

        try {

            String payload = message.getPayload().toString();

            System.out.println(
                    "MQTT Rain Message: " + payload
            );

            RainReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            RainReadingRequest.class
                    );

            rainReadingService.save(request);

            System.out.println(
                    "Rain reading berhasil disimpan."
            );

        } catch (Exception e) {

            System.err.println(
                    "Gagal memproses MQTT rain message: "
                            + e.getMessage()
            );
        }
    }
}