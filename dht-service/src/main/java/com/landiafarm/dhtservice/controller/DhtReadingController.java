
package com.landiafarm.dhtservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.landiafarm.dhtservice.dto.DhtReadingRequest;
import com.landiafarm.dhtservice.entity.DhtReading;
import com.landiafarm.dhtservice.service.DhtReadingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/dht/readings")
public class DhtReadingController {

    private final DhtReadingService service;

    public DhtReadingController(DhtReadingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DhtReading> create(
            @Valid @RequestBody DhtReadingRequest request
    ) {
        return ResponseEntity.ok(
                service.save(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<DhtReading>> findAll() {
        return ResponseEntity.ok(
                service.findAll()
        );
    }

    @GetMapping("/{sensorId}")
    public ResponseEntity<List<DhtReading>> findBySensorId(
            @PathVariable String sensorId
    ) {
        return ResponseEntity.ok(
                service.findBySensorId(sensorId)
        );
    }

    @GetMapping("/{sensorId}/latest")
    public ResponseEntity<DhtReading> findLatest(
            @PathVariable String sensorId
    ) {
        DhtReading latest = service.findLatest(sensorId);

        if (latest == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(latest);
    }
}
