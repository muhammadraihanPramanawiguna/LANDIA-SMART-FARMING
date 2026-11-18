
package com.landiafarm.flowservice.mqtt;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.flowservice.dto.FlowReadingRequest;
import com.landiafarm.flowservice.service.FlowReadingService;

@Component
public class MqttFlowMessageListener {

    private final ObjectMapper objectMapper;
    private final FlowReadingService flowReadingService;
    private final MqttFlowAckPublisher ackPublisher;

    public MqttFlowMessageListener(
            ObjectMapper objectMapper,
            FlowReadingService flowReadingService,
            MqttFlowAckPublisher ackPublisher) {

        this.objectMapper = objectMapper;
        this.flowReadingService = flowReadingService;
        this.ackPublisher = ackPublisher;
    }

    public void handleMessage(Message<?> message) {

        try {
            String payload = message.getPayload().toString();

            System.out.println(
                    "MQTT Flow Message: " + payload
            );

            FlowReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            FlowReadingRequest.class
                    );

            // Simpan data dan tunggu transaksi selesai.
            flowReadingService.save(request);

            String recordId = request.getRecordId();
            String deviceId = request.getDeviceId();

            // Kompatibilitas data lama tanpa recordId.
            if (recordId == null) {
                System.out.println(
                        "Flow reading tersimpan (legacy, tanpa ACK)."
                );
                return;
            }

            if (recordId.isBlank()) {
                throw new IllegalArgumentException(
                        "recordId tidak boleh kosong"
                );
            }

            if (deviceId == null || deviceId.isBlank()) {
                throw new IllegalArgumentException(
                        "deviceId tidak tersedia untuk pengiriman ACK"
                );
            }

            // ACK hanya dikirim setelah penyimpanan berhasil.
            ackPublisher.publishStored(
                    recordId.trim(),
                    deviceId.trim()
            );

            System.out.println(
                    "Flow reading tersimpan dan ACK dikirim: "
                            + recordId
            );

        } catch (Exception e) {
            System.err.println(
                    "Gagal memproses MQTT flow message: "
                            + e.getMessage()
            );
        }
    }
}
