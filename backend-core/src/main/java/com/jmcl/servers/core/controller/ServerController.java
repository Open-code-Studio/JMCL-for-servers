package com.jmcl.servers.core.controller;

import com.jmcl.servers.core.model.ServerInstance;
import com.jmcl.servers.core.model.LogEntry;
import com.jmcl.servers.core.service.ServerInstanceManager;
import com.jmcl.servers.core.service.McServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servers")
@CrossOrigin(origins = "*")
public class ServerController {

    private final ServerInstanceManager instanceManager;
    private final McServerService mcServerService;

    public ServerController(ServerInstanceManager instanceManager, McServerService mcServerService) {
        this.instanceManager = instanceManager;
        this.mcServerService = mcServerService;
    }

    @GetMapping
    public List<ServerInstance> listServers() {
        return instanceManager.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerInstance> getServer(@PathVariable String id) {
        return instanceManager.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteServer(@PathVariable String id) {
        try {
            instanceManager.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startServer(@PathVariable String id) {
        try {
            mcServerService.start(id);
            return ResponseEntity.ok(Map.of("success", true, "status", "RUNNING"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<?> stopServer(@PathVariable String id) {
        try {
            mcServerService.stop(id);
            return ResponseEntity.ok(Map.of("success", true, "status", "STOPPED"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/restart")
    public ResponseEntity<?> restartServer(@PathVariable String id) {
        try {
            mcServerService.restart(id);
            return ResponseEntity.ok(Map.of("success", true, "status", "RUNNING"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/command")
    public ResponseEntity<?> sendCommand(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            String command = body.get("command");
            if (command == null || command.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Command is required"));
            }
            mcServerService.sendCommand(id, command);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<?> getLogs(@PathVariable String id,
                                     @RequestParam(defaultValue = "100") int tail) {
        try {
            List<LogEntry> logs = mcServerService.getLogs(id, tail);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/statuses")
    public Map<String, ServerInstance.ServerStatus> getAllStatuses() {
        return mcServerService.getAllStatuses();
    }
}
