package com.jmcl.servers.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerType {
    private String id;
    private String name;            // display name
    private String category;        // VANILLA, MODDED, PROXY, HYBRID
    private String apiBase;
    private String description;
    private List<String> supportedVersions;
    private Map<String, String> downloadTemplates;
}
