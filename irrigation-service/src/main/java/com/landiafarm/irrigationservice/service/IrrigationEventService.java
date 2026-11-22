package com.landiafarm.irrigationservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landiafarm.irrigationservice.dto.IrrigationEventRequest;
import com.landiafarm.irrigationservice.entity.IrrigationEvent;
import com.landiafarm.irrigationservice.exception.IrrigationEventConflictException;
import com.landiafarm.irrigationservice.repository.IrrigationEventRepository;

@Service
public class IrrigationEventService {

    private final IrrigationEventRepository repository;

    public IrrigationEventService(
            IrrigationEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IrrigationEvent save(IrrigationEventRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                "Irrigation event request tidak boleh null"
            );
        }

        String recordId = normalize(request.getRecordId());
        String deviceId = normalize(request.getDeviceId());
        String event = normalize(request.getEvent());
        String state = normalize(request.getState());

        if (recordId == null || deviceId == null ||
            event == null || state == null) {
            throw new IllegalArgumentException(
                "recordId, deviceId, event, dan state wajib diisi"
            );
        }

        if (recordId.length() > 40 || deviceId.length() > 100) {
            throw new IllegalArgumentException(
                "Panjang recordId atau deviceId melebihi batas"
            );
        }

        if (!"IRRIGATION".equals(event)) {
            throw new IllegalArgumentException(
                "Event yang didukung hanya IRRIGATION"
            );
        }

        LocalDateTime receivedAt = LocalDateTime.now();

        repository.insertIfAbsent(
            recordId,
            deviceId,
            event,
            state,
            request.getTimestamp(),
            receivedAt
        );

        IrrigationEvent saved = repository
            .findByRecordId(recordId)
            .orElseThrow(() -> new IllegalStateException(
                "Event irigasi tidak ditemukan setelah proses insert"
            ));

        /*
         * recordId sama + payload sama = duplikat yang aman.
         * recordId sama + payload berbeda = konflik.
         */
        boolean samePayload =
            saved.getDeviceId().equals(deviceId)
            && saved.getEvent().equals(event)
            && saved.getState().equals(state)
            && java.util.Objects.equals(
                saved.getTimestamp(),
                request.getTimestamp()
            );

        if (!samePayload) {
            throw new IrrigationEventConflictException(
                "recordId sudah digunakan untuk payload irigasi berbeda"
            );
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<IrrigationEvent> findAll() {
        return repository.findAllByOrderByReceivedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<IrrigationEvent> findByDeviceId(String deviceId) {

        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException(
                "deviceId tidak boleh kosong"
            );
        }

        return repository.findByDeviceIdOrderByReceivedAtDesc(
            deviceId.trim()
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
            ? null
            : value.trim();
    }
}