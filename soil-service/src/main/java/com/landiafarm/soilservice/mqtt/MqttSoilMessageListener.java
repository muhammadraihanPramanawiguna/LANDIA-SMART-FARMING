
package com.landiafarm.soilservice.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.soilservice.dto.SoilReadingRequest;
import com.landiafarm.soilservice.service.SoilReadingService;

@Component
public class MqttSoilMessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(MqttSoilMessageListener.class);

    private final SoilReadingService soilReadingService;
    private final MqttSoilAckPublisher ackPublisher;
    private final ObjectMapper objectMapper;

    public MqttSoilMessageListener(
            SoilReadingService soilReadingService,
            MqttSoilAckPublisher ackPublisher,
            ObjectMapper objectMapper) {

        this.soilReadingService = soilReadingService;
        this.ackPublisher = ackPublisher;
        this.objectMapper = objectMapper;
    }

    public void handleMessage(Message<?> message) {

        try {
            String payload = message.getPayload().toString();

            SoilReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            SoilReadingRequest.class
                    );

            // Simpan data terlebih dahulu.
            // Method @Transactional akan commit sebelum kembali ke sini.
            soilReadingService.saveReading(request);

            // Data lama tetap diproses, tetapi tidak memerlukan ACK.
            if (request.getRecordId() == null
                    || request.getRecordId().isBlank()) {
                log.info("Legacy soil message stored without application ACK");
                return;
            }

            String recordId = request.getRecordId().trim();

            // ACK baru dikirim setelah penyimpanan berhasil.
            ackPublisher.publishStored(recordId);

            log.info("Soil record stored and ACK published: {}", recordId);

        } catch (Exception e) {
            log.error("Failed to process MQTT soil message", e);
        }
    }
}
