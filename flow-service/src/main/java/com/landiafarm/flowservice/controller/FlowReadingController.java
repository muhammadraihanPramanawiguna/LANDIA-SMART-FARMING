package com.landiafarm.flowservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.landiafarm.flowservice.dto.FlowReadingRequest;
import com.landiafarm.flowservice.entity.FlowReading;
import com.landiafarm.flowservice.service.FlowReadingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/flow")
public class FlowReadingController {

    private final FlowReadingService flowReadingService;

    public FlowReadingController(
            FlowReadingService flowReadingService) {

        this.flowReadingService = flowReadingService;
    }

    @PostMapping("/readings")
    public ResponseEntity<FlowReading> createReading(
            @Valid @RequestBody FlowReadingRequest request) {

        FlowReading saved =
                flowReadingService.save(request);

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/readings")
    public ResponseEntity<List<FlowReading>> getAllReadings() {

        return ResponseEntity.ok(
                flowReadingService.findAll()
        );
    }

    @GetMapping("/readings/{sensorId}")
    public ResponseEntity<List<FlowReading>> getBySensorId(
            @PathVariable String sensorId) {

        return ResponseEntity.ok(
                flowReadingService.findBySensorId(sensorId)
        );
    }

    @GetMapping("/readings/{sensorId}/latest")
    public ResponseEntity<FlowReading> getLatest(
            @PathVariable String sensorId) {

        FlowReading latest =
                flowReadingService.findLatest(sensorId);

        if (latest == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(latest);
    }
}