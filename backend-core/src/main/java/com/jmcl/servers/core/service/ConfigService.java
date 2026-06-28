package com.jmcl.servers.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jmcl.servers.core.model.ServerConfig;
import com.jmcl.servers.core.model.ServerInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private final ServerInstanceManager instanceManager;

    public ConfigService(ServerInstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    public ServerConfig getConfig(String serverId) throws IOException {
        ServerInstance instance = instanceManager.getById(serverId)
                .orElseThrow(() -> new NoSuchElementException("Server not found: " + serverId));
        Path dir = Path.of(instance.getDataDir());

        ServerConfig config = ServerConfig.builder()
                .serverId(serverId)
                .properties(readProperties(dir))
                .eula(readFile(dir.resolve("eula.txt")))
                .ops(readFile(dir.resolve("ops.json")))
                .whitelist(readFile(dir.resolve("whitelist.json")))
                .bannedPlayers(readFile(dir.resolve("banned-players.json")))
                .bannedIps(readFile(dir.resolve("banned-ips.json")))
                .build();

        // Spigot/Bukkit config
        Path bukkitYml = dir.resolve("bukkit.yml");
        Path spigotYml = dir.resolve("spigot.yml");
        if (Files.exists(spigotYml)) {
            config.setBukkitConfig(Files.readString(spigotYml));
        } else if (Files.exists(bukkitYml)) {
            config.setBukkitConfig(Files.readString(bukkitYml));
        }

        return config;
    }

    private Map<String, String> readProperties(Path dir) throws IOException {
        Path propsFile = dir.resolve("server.properties");
        Map<String, String> props = new LinkedHashMap<>();
        if (Files.exists(propsFile)) {
            Properties javaProps = new Properties();
            try (var reader = Files.newBufferedReader(propsFile)) {
                javaProps.load(reader);
            }
            javaProps.stringPropertyNames().forEach(k ->
                    props.put(k, javaProps.getProperty(k)));
        }
        return props;
    }

    private String readFile(Path path) throws IOException {
        return Files.exists(path) ? Files.readString(path) : null;
    }

    public ServerConfig updateConfig(String serverId, ServerConfig config) throws IOException {
        ServerInstance instance = instanceManager.getById(serverId)
                .orElseThrow(() -> new NoSuchElementException("Server not found: " + serverId));
        Path dir = Path.of(instance.getDataDir());

        if (config.getProperties() != null && !config.getProperties().isEmpty()) {
            writeProperties(dir, config.getProperties());
            // Also update the instance properties
            instance.setProperties(config.getProperties());
        }

        if (config.getEula() != null) {
            Files.writeString(dir.resolve("eula.txt"), config.getEula());
        }
        if (config.getOps() != null) {
            Files.writeString(dir.resolve("ops.json"), config.getOps());
        }
        if (config.getWhitelist() != null) {
            Files.writeString(dir.resolve("whitelist.json"), config.getWhitelist());
        }
        if (config.getBannedPlayers() != null) {
            Files.writeString(dir.resolve("banned-players.json"), config.getBannedPlayers());
        }
        if (config.getBannedIps() != null) {
            Files.writeString(dir.resolve("banned-ips.json"), config.getBannedIps());
        }

        log.info("Updated config for server: {}", serverId);
        return getConfig(serverId);
    }

    private void writeProperties(Path dir, Map<String, String> props) throws IOException {
        Properties javaProps = new Properties();
        javaProps.putAll(props);
        try (var writer = Files.newBufferedWriter(dir.resolve("server.properties"))) {
            javaProps.store(writer, "JMCL Server Properties");
        }
    }

    public Map<String, String> getDefaultProperties() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("server-port", "25565");
        defaults.put("max-players", "20");
        defaults.put("view-distance", "10");
        defaults.put("simulation-distance", "10");
        defaults.put("online-mode", "true");
        defaults.put("allow-flight", "false");
        defaults.put("allow-nether", "true");
        defaults.put("announce-player-achievements", "true");
        defaults.put("difficulty", "easy");
        defaults.put("gamemode", "survival");
        defaults.put("generate-structures", "true");
        defaults.put("hardcore", "false");
        defaults.put("level-name", "world");
        defaults.put("level-type", "minecraft:default");
        defaults.put("motd", "A JMCL Minecraft Server");
        defaults.put("pvp", "true");
        defaults.put("spawn-protection", "16");
        defaults.put("white-list", "false");
        defaults.put("enable-command-block", "false");
        defaults.put("spawn-monsters", "true");
        defaults.put("spawn-animals", "true");
        defaults.put("snooper-enabled", "true");
        return defaults;
    }
}
