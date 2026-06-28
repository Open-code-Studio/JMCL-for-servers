package com.jmcl.servers.core.controller;

import com.jmcl.servers.core.model.ServerConfig;
import com.jmcl.servers.core.model.ServerInstance;
import com.jmcl.servers.core.service.ConfigService;
import com.jmcl.servers.core.service.ServerInstanceManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    private final ConfigService configService;
    private final ServerInstanceManager instanceManager;

    public ConfigController(ConfigService configService, ServerInstanceManager instanceManager) {
        this.configService = configService;
        this.instanceManager = instanceManager;
    }

    @GetMapping("/{serverId}")
    public ResponseEntity<?> getConfig(@PathVariable String serverId) {
        try {
            ServerConfig config = configService.getConfig(serverId);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{serverId}")
    public ResponseEntity<?> updateConfig(@PathVariable String serverId, @RequestBody ServerConfig config) {
        try {
            ServerConfig updated = configService.updateConfig(serverId, config);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{serverId}/defaults")
    public Map<String, String> getDefaultProperties() {
        return configService.getDefaultProperties();
    }

    @PutMapping("/{serverId}/instance")
    public ResponseEntity<?> updateInstance(@PathVariable String serverId,
                                            @RequestBody ServerInstance updates) {
        try {
            ServerInstance updated = instanceManager.update(serverId, updates);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
