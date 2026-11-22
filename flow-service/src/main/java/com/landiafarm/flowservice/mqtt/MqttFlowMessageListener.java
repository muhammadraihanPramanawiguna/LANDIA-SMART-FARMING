
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

            // 1. Pastikan MQTT manual ACK tersedia.
            SimpleAcknowledgment acknowledgment =
                    StaticMessageHeaderAccessor.getAcknowledgment(message);

            if (acknowledgment == null) {
                throw new IllegalStateException(
                        "MQTT manual acknowledgment callback tidak tersedia"
                );
            }

            // 2. Ambil payload MQTT.
            payload = extractPayload(message);

            if (payload.isBlank()) {
                throw new IllegalArgumentException(
                        "MQTT payload tidak boleh kosong"
                );
            }

            // 3. Deserialisasi JSON.
            FlowReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            FlowReadingRequest.class
                    );

            if (request == null) {
                throw new IllegalArgumentException(
                        "Payload Flow tidak valid"
                );
            }

            // 4. Normalisasi identitas.
            recordId = normalize(request.getRecordId());
            deviceId = normalize(request.getDeviceId());

            request.setRecordId(recordId);
            request.setDeviceId(deviceId);

            // 5. Validasi hubungan recordId dan deviceId.
            if (recordId != null && deviceId == null) {
                throw new IllegalArgumentException(
                        "deviceId wajib diisi jika recordId diberikan"
                );
            }

            // 6. Jalankan Bean Validation.
            validateRequest(request);

            // 7. Simpan atau verifikasi data melalui service.
            // Service menangani idempotensi dan konflik recordId.
            flowReadingService.save(request);

            log.info(
                    "Flow data berhasil diproses. recordId={}, deviceId={}, sensorId={}",
                    recordId,
                    deviceId,
                    request.getSensorId()
            );

            // 8. Kirim Application ACK setelah proses database berhasil.
            if (recordId != null) {

                ackPublisher.publishStored(
                        recordId,
                        deviceId
                );

                log.info(
                        "Flow Application ACK published. recordId={}, deviceId={}",
                        recordId,
                        deviceId
                );

            } else {

                log.warn(
                        "Legacy Flow payload diproses tanpa Application ACK. sensorId={}",
                        request.getSensorId()
                );
            }

            // 9. Selesaikan MQTT manual ACK.
            acknowledgment.acknowledge();

            log.info(
                    "Flow MQTT message acknowledged. recordId={}",
                    recordId
            );

        } catch (Exception exception) {

            // Jangan menyelesaikan MQTT ACK jika proses gagal.
            // Redelivery bergantung pada konfigurasi session dan broker.

            log.error(
                    "Gagal memproses Flow MQTT. recordId={}, deviceId={}, payload={}. MQTT ACK belum diselesaikan.",
                    recordId,
                    deviceId,
                    preview(payload),
                    exception
            );
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
                                + violation.getMessage()
                );
            }

            throw new IllegalArgumentException(
                    "Validasi payload Flow gagal: " + errors
            );
        }
    }

    private String extractPayload(Message<?> message) {

        if (message == null || message.getPayload() == null) {
            throw new IllegalArgumentException(
                    "MQTT message/payload tidak boleh null"
            );
        }

        Object raw = message.getPayload();

        if (raw instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        if (raw instanceof String text) {
            return text;
        }

        throw new IllegalArgumentException(
                "Tipe payload MQTT tidak didukung: "
                        + raw.getClass().getName()
        );
    }

    private String normalize(String value) {

        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private String preview(String payload) {

        if (payload == null) {
            return "null";
        }

        return payload.length() <= 300
                ? payload
                : payload.substring(0, 300) + "...";
    }
}
