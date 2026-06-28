package com.jmcl.servers.core.controller;

import com.jmcl.servers.core.model.DownloadRequest;
import com.jmcl.servers.core.model.ServerType;
import com.jmcl.servers.core.service.DownloadService;
import com.jmcl.servers.core.service.ServerTypeRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/download")
@CrossOrigin(origins = "*")
public class DownloadController {

    private final DownloadService downloadService;
    private final ServerTypeRegistry typeRegistry;

    public DownloadController(DownloadService downloadService, ServerTypeRegistry typeRegistry) {
        this.downloadService = downloadService;
        this.typeRegistry = typeRegistry;
    }

    @GetMapping("/types")
    public List<ServerType> listTypes() {
        return typeRegistry.getAll();
    }

    @GetMapping("/types/{id}")
    public ResponseEntity<ServerType> getType(@PathVariable String id) {
        return typeRegistry.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/types/category/{category}")
    public List<ServerType> listByCategory(@PathVariable String category) {
        return typeRegistry.getByCategory(category);
    }

    @PostMapping("/start")
    public ResponseEntity<?> startDownload(@RequestBody DownloadRequest request) {
        String downloadId = UUID.randomUUID().toString().substring(0, 8);
        downloadService.downloadServer(request);
        // The actual download runs async; return immediately
        return ResponseEntity.accepted().body(Map.of(
                "success", true,
                "message", "Download started",
                "downloadId", downloadId
        ));
    }

    @GetMapping("/progress/{downloadId}")
    public ResponseEntity<?> getProgress(@PathVariable String downloadId) {
        String progress = downloadService.getProgress(downloadId);
        return ResponseEntity.ok(Map.of("progress", progress));
    }
}
