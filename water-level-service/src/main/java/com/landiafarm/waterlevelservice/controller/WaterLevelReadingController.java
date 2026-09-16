package com.landiafarm.waterlevelservice.controller;

import com.landiafarm.waterlevelservice.dto.WaterLevelReadingRequest;
import com.landiafarm.waterlevelservice.entity.WaterLevelReading;
import com.landiafarm.waterlevelservice.service.WaterLevelReadingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/water-level/readings")
public class WaterLevelReadingController {

    private final WaterLevelReadingService service;

    public WaterLevelReadingController(
            WaterLevelReadingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<WaterLevelReading> create(
            @Valid @RequestBody WaterLevelReadingRequest request) {

        return ResponseEntity.ok(service.save(request));
    }

    @GetMapping
    public ResponseEntity<List<WaterLevelReading>> findAll() {

        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{sensorId}")
    public ResponseEntity<List<WaterLevelReading>> findBySensorId(
            @PathVariable String sensorId) {

        return ResponseEntity.ok(
                service.findBySensorId(sensorId)
        );
    }

    @GetMapping("/{sensorId}/latest")
    public ResponseEntity<WaterLevelReading> findLatest(
            @PathVariable String sensorId) {

        WaterLevelReading latest =
                service.findLatest(sensorId);

        if (latest == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(latest);
    }
}