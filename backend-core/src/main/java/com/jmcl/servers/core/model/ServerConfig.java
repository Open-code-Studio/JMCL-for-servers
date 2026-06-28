package com.jmcl.servers.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerConfig {
    private String serverId;
    private Map<String, String> properties;   // server.properties
    private String ops;                        // ops.json content
    private String whitelist;                  // whitelist.json content
    private String bannedPlayers;              // banned-players.json
    private String bannedIps;                  // banned-ips.json
    private String eula;                       // eula.txt (must be true)
    private String bukkitConfig;               // bukkit.yml / spigot.yml
}
