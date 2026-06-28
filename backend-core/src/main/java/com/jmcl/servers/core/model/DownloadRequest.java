package com.jmcl.servers.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadRequest {
    private String type;          // VANILLA, PAPER, FABRIC, FORGE, FABRIC, PURPUR
    private String mcVersion;     // e.g., "1.20.4"
    private String loader;        // Fabric/Quilt loader version
    private String buildId;       // Paper/Purpur build number
    private String serverName;    // New server instance name
}
