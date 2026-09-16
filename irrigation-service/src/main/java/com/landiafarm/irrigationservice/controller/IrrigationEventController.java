package com.landiafarm.irrigationservice.controller;

import com.landiafarm.irrigationservice.dto.IrrigationEventRequest;
import com.landiafarm.irrigationservice.entity.IrrigationEvent;
import com.landiafarm.irrigationservice.service.IrrigationEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/irrigation/events")
public class IrrigationEventController {

    private final IrrigationEventService service;

    public IrrigationEventController(
            IrrigationEventService service) {

        this.service = service;
    }

    @PostMapping
    public ResponseEntity<IrrigationEvent> execute(
            @Valid @RequestBody IrrigationEventRequest request) {

        return ResponseEntity.ok(
                service.execute(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<IrrigationEvent>> findAll() {

        return ResponseEntity.ok(
                service.findAll()
        );
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<List<IrrigationEvent>> findByDeviceId(
            @PathVariable String deviceId) {

        return ResponseEntity.ok(
                service.findByDeviceId(deviceId)
        );
    }
}