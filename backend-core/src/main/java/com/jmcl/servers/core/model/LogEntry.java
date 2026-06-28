package com.jmcl.servers.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    private String serverId;
    private String timestamp;
    private String level;    // INFO, WARN, ERROR
    private String message;
}
