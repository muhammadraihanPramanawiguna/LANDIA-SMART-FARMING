package com.landiafarm.dhtservice.mqtt;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.dhtservice.dto.DhtReadingRequest;
import com.landiafarm.dhtservice.service.DhtReadingService;

@Component
public class MqttDhtMessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(MqttDhtMessageListener.class);

    private final ObjectMapper objectMapper;
    private final DhtReadingService service;
    private final MqttDhtAckPublisher ackPublisher;

    public MqttDhtMessageListener(
            ObjectMapper objectMapper,
            DhtReadingService service,
            MqttDhtAckPublisher ackPublisher) {
        this.objectMapper = objectMapper;
        this.service = service;
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
                throw new IllegalArgumentException("Payload DHT kosong");
            }

            DhtReadingRequest request =
                    objectMapper.readValue(payload, DhtReadingRequest.class);

            if (request == null) {
                throw new IllegalArgumentException("DHT request null");
            }

            recordId = normalize(request.getRecordId());
            deviceId = normalize(request.getDeviceId());

            requireIdentity(recordId, deviceId);

            request.setRecordId(recordId);
            request.setDeviceId(deviceId);

            service.save(request);

            ackPublisher.publishStored(recordId, deviceId);

            acknowledgment.acknowledge();

            log.info(
                    "DHT MQTT berhasil disimpan dan di-ACK. recordId={}, deviceId={}",
                    recordId, deviceId);

        } catch (Exception e) {
            log.error(
                    "Gagal memproses DHT MQTT. recordId={}, deviceId={}, payload={}",
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