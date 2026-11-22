package com.landiafarm.irrigationservice.mqtt;

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
import com.landiafarm.irrigationservice.dto.IrrigationEventRequest;
import com.landiafarm.irrigationservice.service.IrrigationEventService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Component
public class MqttIrrigationEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(MqttIrrigationEventListener.class);

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final IrrigationEventService irrigationEventService;
    private final MqttIrrigationAckPublisher ackPublisher;

    public MqttIrrigationEventListener(
            ObjectMapper objectMapper,
            Validator validator,
            IrrigationEventService irrigationEventService,
            MqttIrrigationAckPublisher ackPublisher) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.irrigationEventService = irrigationEventService;
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
                throw new IllegalArgumentException("Payload Irrigation kosong");
            }

            IrrigationEventRequest request =
                    objectMapper.readValue(
                            payload, IrrigationEventRequest.class);

            if (request == null) {
                throw new IllegalArgumentException(
                        "Irrigation request null");
            }

            recordId = normalize(request.getRecordId());
            deviceId = normalize(request.getDeviceId());

            if (recordId == null || deviceId == null) {
                throw new IllegalArgumentException(
                        "recordId dan deviceId wajib diisi");
            }

            request.setRecordId(recordId);
            request.setDeviceId(deviceId);

            validateRequest(request);

            irrigationEventService.save(request);

            ackPublisher.publishStored(recordId, deviceId);

            acknowledgment.acknowledge();

            log.info(
                    "Irrigation MQTT berhasil disimpan dan di-ACK. recordId={}, deviceId={}",
                    recordId, deviceId);

        } catch (Exception e) {
            log.error(
                    "Gagal memproses Irrigation MQTT. recordId={}, deviceId={}, payload={}",
                    recordId, deviceId, preview(payload), e);
        }
    }

    private void validateRequest(IrrigationEventRequest request) {
        Set<ConstraintViolation<IrrigationEventRequest>> violations =
                validator.validate(request);

        if (!violations.isEmpty()) {
            StringJoiner errors = new StringJoiner("; ");

            for (ConstraintViolation<IrrigationEventRequest> violation : violations) {
                errors.add(
                        violation.getPropertyPath()
                                + ": "
                                + violation.getMessage());
            }

            throw new IllegalArgumentException(
                    "Validasi Irrigation gagal: " + errors);
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

    private String preview(String payload) {
        if (payload == null) return "null";
        return payload.length() <= 300
                ? payload : payload.substring(0, 300) + "...";
    }
}