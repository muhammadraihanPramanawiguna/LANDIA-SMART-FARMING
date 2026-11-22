package com.landiafarm.flowservice.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.flowservice.dto.FlowReadingRequest;
import com.landiafarm.flowservice.service.FlowReadingService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Component
public class MqttFlowMessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(MqttFlowMessageListener.class);

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final FlowReadingService flowReadingService;
    private final MqttFlowAckPublisher ackPublisher;

    public MqttFlowMessageListener(
            ObjectMapper objectMapper,
            Validator validator,
            FlowReadingService flowReadingService,
            MqttFlowAckPublisher ackPublisher) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.flowReadingService = flowReadingService;
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
                throw new IllegalArgumentException("Payload Flow kosong");
            }

            FlowReadingRequest request =
                    objectMapper.readValue(payload, FlowReadingRequest.class);

            if (request == null) {
                throw new IllegalArgumentException("Flow request null");
            }

            recordId = normalize(request.getRecordId());
            deviceId = normalize(request.getDeviceId());

            requireIdentity(recordId, deviceId);

            request.setRecordId(recordId);
            request.setDeviceId(deviceId);

            validateRequest(request);

            flowReadingService.save(request);

            ackPublisher.publishStored(recordId, deviceId);

            acknowledgment.acknowledge();

            log.info(
                    "Flow MQTT berhasil disimpan dan di-ACK. recordId={}, deviceId={}",
                    recordId, deviceId);

        } catch (Exception e) {
            log.error(
                    "Gagal memproses Flow MQTT. recordId={}, deviceId={}, payload={}",
                    recordId, deviceId, preview(payload), e);
        }
    }

    private void validateRequest(FlowReadingRequest request) {
        Set<ConstraintViolation<FlowReadingRequest>> violations =
                validator.validate(request);

        if (!violations.isEmpty()) {
            StringJoiner errors = new StringJoiner("; ");

            for (ConstraintViolation<FlowReadingRequest> violation : violations) {
                errors.add(
                        violation.getPropertyPath()
                                + ": "
                                + violation.getMessage());
            }

            throw new IllegalArgumentException(
                    "Validasi Flow gagal: " + errors);
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