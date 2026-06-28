package com.jmcl.servers.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerInstance {
    private String id;
    private String name;
    private String type;          // VANILLA, PAPER, FABRIC, FORGE, PURPUR, FOLIA, etc.
    private String mcVersion;     // e.g., "1.20.4"
    private String serverJar;     // path to server jar
    private ServerStatus status;  // STOPPED, STARTING, RUNNING, STOPPING, ERROR
    private int port;
    private int maxRam;           // MB
    private int minRam;           // MB
    private String javaArgs;
    private String dataDir;
    private long pid;
    private LocalDateTime createdAt;
    private LocalDateTime lastStartedAt;
    private Map<String, String> properties;  // server.properties
    private String eulaAccepted;

    public enum ServerStatus {
        STOPPED, STARTING, RUNNING, STOPPING, ERROR
    }
}
