package com.landiafarm.rainservice.mqtt;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.rainservice.dto.RainReadingRequest;
import com.landiafarm.rainservice.service.RainReadingService;

@Component
public class MqttRainMessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(MqttRainMessageListener.class);

    private final ObjectMapper objectMapper;
    private final RainReadingService rainReadingService;
    private final MqttRainAckPublisher ackPublisher;

    public MqttRainMessageListener(
            ObjectMapper objectMapper,
            RainReadingService rainReadingService,
            MqttRainAckPublisher ackPublisher) {
        this.objectMapper = objectMapper;
        this.rainReadingService = rainReadingService;
        this.ackPublisher = ackPublisher;
    }

    public void handleMessage(Message<?> message) {

        String recordId = null;
        String deviceId = null;
        String payload = null;

        try {
            SimpleAcknowledgment acknowledgment =
                    StaticMessageHeaderAccessor.getAcknowledgment(message);

            if (acknowledgment == null) {
                throw new IllegalStateException(
                        "MQTT manual acknowledgment tidak tersedia");
            }

            payload = cleanPayload(extractPayload(message));

            if (payload.isBlank()) {
                throw new IllegalArgumentException("Payload Rain kosong");
            }

            RainReadingRequest request =
                    objectMapper.readValue(payload, RainReadingRequest.class);

            if (request == null) {
                throw new IllegalArgumentException("Rain request null");
            }

            recordId = normalize(request.getRecordId());
            deviceId = normalize(request.getDeviceId());

            requireIdentity(recordId, deviceId);

            request.setRecordId(recordId);
            request.setDeviceId(deviceId);

            rainReadingService.save(request);

            ackPublisher.publishStored(recordId, deviceId);

            acknowledgment.acknowledge();

            log.info(
                    "Rain MQTT berhasil disimpan dan di-ACK. recordId={}, deviceId={}",
                    recordId, deviceId);

        } catch (Exception e) {
            log.error(
                    "Gagal memproses Rain MQTT. recordId={}, deviceId={}, payload={}",
                    recordId, deviceId, preview(payload), e);
        }
    }

    private String extractPayload(Message<?> message) {
        if (message == null || message.getPayload() == null) {
            throw new IllegalArgumentException("MQTT message/payload null");
        }

        Object raw = message.getPayload();

        if (raw instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        if (raw instanceof String text) {
            return text;
        }

        throw new IllegalArgumentException("Tipe payload MQTT tidak didukung");
    }

    private String cleanPayload(String payload) {
        return payload.replace("\uFEFF", "").trim();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireIdentity(String recordId, String deviceId) {
        if (recordId == null || deviceId == null) {
            throw new IllegalArgumentException(
                    "recordId dan deviceId wajib diisi");
        }
    }

    private String preview(String payload) {
        if (payload == null) return "null";
        return payload.length() <= 300
                ? payload : payload.substring(0, 300) + "...";
    }
}