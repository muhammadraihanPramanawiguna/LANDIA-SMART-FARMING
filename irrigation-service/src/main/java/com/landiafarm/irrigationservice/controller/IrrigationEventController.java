
package com.landiafarm.irrigationservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.landiafarm.irrigationservice.dto.IrrigationEventRequest;
import com.landiafarm.irrigationservice.entity.IrrigationEvent;
import com.landiafarm.irrigationservice.service.IrrigationEventService;

import jakarta.validation.Valid;

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

        return ResponseEntity.accepted().body(
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
