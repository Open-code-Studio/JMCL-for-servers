package com.jmcl.servers.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jmcl.servers.core.model.ServerConfig;
import com.jmcl.servers.core.model.ServerInstance;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServerInstanceManager {

    private static final Logger log = LoggerFactory.getLogger(ServerInstanceManager.class);
    private final Map<String, ServerInstance> instances = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jmcl.data.dir:/data}")
    private String dataDir;

    private Path instancesPath;

    @PostConstruct
    public void init() throws IOException {
        instancesPath = Path.of(dataDir, "instances");
        Files.createDirectories(instancesPath);
        loadInstances();
    }

    private void loadInstances() throws IOException {
        Path indexFile = instancesPath.resolve("index.json");
        if (Files.exists(indexFile)) {
            ServerInstance[] loaded = objectMapper.readValue(indexFile.toFile(), ServerInstance[].class);
            for (ServerInstance s : loaded) {
                instances.put(s.getId(), s);
            }
            log.info("Loaded {} server instances", instances.size());
        }
    }

    private void saveIndex() throws IOException {
        objectMapper.writeValue(instancesPath.resolve("index.json").toFile(),
                new ArrayList<>(instances.values()));
    }

    public List<ServerInstance> listAll() {
        return new ArrayList<>(instances.values());
    }

    public Optional<ServerInstance> getById(String id) {
        return Optional.ofNullable(instances.get(id));
    }

    public ServerInstance create(ServerInstance instance) throws IOException {
        instance.setId(UUID.randomUUID().toString().substring(0, 8));
        instance.setStatus(ServerInstance.ServerStatus.STOPPED);
        instance.setCreatedAt(java.time.LocalDateTime.now());
        instance.setDataDir(dataDir + "/servers/" + instance.getId());
        Files.createDirectories(Path.of(instance.getDataDir()));
        instances.put(instance.getId(), instance);
        saveIndex();
        log.info("Created server instance: {} ({})", instance.getName(), instance.getId());
        return instance;
    }

    public ServerInstance update(String id, ServerInstance updated) throws IOException {
        ServerInstance existing = instances.get(id);
        if (existing == null) throw new NoSuchElementException("Server not found: " + id);
        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getMaxRam() > 0) existing.setMaxRam(updated.getMaxRam());
        if (updated.getMinRam() > 0) existing.setMinRam(updated.getMinRam());
        if (updated.getJavaArgs() != null) existing.setJavaArgs(updated.getJavaArgs());
        if (updated.getPort() > 0) existing.setPort(updated.getPort());
        if (updated.getServerJar() != null) existing.setServerJar(updated.getServerJar());
        if (updated.getProperties() != null) {
            if (existing.getProperties() == null) existing.setProperties(new HashMap<>());
            existing.getProperties().putAll(updated.getProperties());
        }
        saveIndex();
        return existing;
    }

    public void delete(String id) throws IOException {
        ServerInstance existing = instances.remove(id);
        if (existing == null) throw new NoSuchElementException("Server not found: " + id);
        Path serverPath = Path.of(existing.getDataDir());
        if (Files.exists(serverPath)) {
            try (var stream = Files.walk(serverPath)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                        });
            }
        }
        saveIndex();
        log.info("Deleted server instance: {} ({})", existing.getName(), id);
    }

    public void updateStatus(String id, ServerInstance.ServerStatus status) throws IOException {
        ServerInstance instance = instances.get(id);
        if (instance != null) {
            instance.setStatus(status);
            if (status == ServerInstance.ServerStatus.RUNNING) {
                instance.setLastStartedAt(java.time.LocalDateTime.now());
            }
            saveIndex();
        }
    }
}
