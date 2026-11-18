
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

        String recordId = null;

        try {
            String payload = message.getPayload().toString();

            SoilReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            SoilReadingRequest.class
                    );

            // Simpan dan selesaikan transaksi database terlebih dahulu.
            soilReadingService.saveReading(request);

            recordId = request.getRecordId();

            // Pesan legacy tanpa recordId tidak memerlukan application ACK.
            if (recordId == null || recordId.isBlank()) {
                log.info("Legacy soil message stored without application ACK");
                return;
            }

            String deviceId = request.getDeviceId();

            if (deviceId == null || deviceId.isBlank()) {
                log.error(
                        "Soil record stored but ACK cannot be published: deviceId is missing, recordId={}",
                        recordId
                );
                return;
            }

            recordId = recordId.trim();
            deviceId = deviceId.trim();

            // ACK dikirim setelah saveReading selesai.
            ackPublisher.publishStored(recordId, deviceId);

            log.info(
                    "Soil record stored and ACK published: recordId={}",
                    recordId
            );

        } catch (Exception e) {
            log.error(
                    "Failed to process MQTT soil message. recordId={}. ACK not confirmed.",
                    recordId,
                    e
            );
        }
    }
}
