package com.jmcl.servers.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmcl.servers.core.model.DownloadRequest;
import com.jmcl.servers.core.model.ServerInstance;
import com.jmcl.servers.core.model.ServerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.*;

@Service
public class DownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);
    private final ServerTypeRegistry typeRegistry;
    private final ServerInstanceManager instanceManager;
    private final ConfigService configService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder().build();

    // Track download progress
    private final Map<String, String> progressMap = new HashMap<>();

    public DownloadService(ServerTypeRegistry typeRegistry, ServerInstanceManager instanceManager,
                           ConfigService configService) {
        this.typeRegistry = typeRegistry;
        this.instanceManager = instanceManager;
        this.configService = configService;
    }

    @Async
    public void downloadServer(DownloadRequest request) {
        String downloadId = UUID.randomUUID().toString().substring(0, 8);
        progressMap.put(downloadId, "STARTING");
        try {
            progressMap.put(downloadId, "FETCHING_METADATA");

            ServerType type = typeRegistry.getById(request.getType())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown server type: " + request.getType()));

            // Create server instance first
            ServerInstance instance = ServerInstance.builder()
                    .name(request.getServerName() != null ? request.getServerName() :
                            type.getName() + " " + request.getMcVersion())
                    .type(type.getId())
                    .mcVersion(request.getMcVersion())
                    .port(25565)
                    .maxRam(2048)
                    .minRam(1024)
                    .properties(configService.getDefaultProperties())
                    .build();

            instance = instanceManager.create(instance);
            Path serverDir = Path.of(instance.getDataDir());
            Files.createDirectories(serverDir);

            // Download based on server type
            Path jarPath = switch (type.getId()) {
                case "VANILLA" -> downloadVanilla(request, type, serverDir);
                case "PAPER" -> downloadPaper(request, type, serverDir);
                case "PURPUR" -> downloadPurpur(request, type, serverDir);
                case "FOLIA" -> downloadFolia(request, type, serverDir);
                case "FABRIC" -> downloadFabric(request, type, serverDir);
                case "QUILT" -> downloadQuilt(request, type, serverDir);
                case "FORGE" -> downloadForge(request, type, serverDir);
                case "NEOFORGE" -> downloadNeoForge(request, type, serverDir);
                case "VELOCITY" -> downloadVelocity(request, type, serverDir);
                default -> throw new IllegalArgumentException("Download not implemented for: " + type.getId());
            };

            instance.setServerJar(jarPath.toString());
            instanceManager.update(instance.getId(), instance);

            // Create eula.txt
            Files.writeString(serverDir.resolve("eula.txt"), "eula=true\n");

            // Create default server.properties
            configService.updateConfig(instance.getId(),
                    com.jmcl.servers.core.model.ServerConfig.builder()
                            .properties(configService.getDefaultProperties())
                            .eula("eula=true\n")
                            .build());

            progressMap.put(downloadId, "COMPLETED:" + instance.getId());
            log.info("Download completed: {} -> {}", type.getId(), instance.getId());

        } catch (Exception e) {
            progressMap.put(downloadId, "ERROR:" + e.getMessage());
            log.error("Download failed", e);
        }
    }

    private Path downloadVanilla(DownloadRequest req, ServerType type, Path dir) throws Exception {
        // Fetch version manifest
        String manifestUrl = type.getDownloadTemplates().get("manifest");
        String manifestJson = webClient.get().uri(manifestUrl).retrieve().bodyToMono(String.class).block();
        JsonNode manifest = objectMapper.readTree(manifestJson);

        // Find version info
        JsonNode versionInfo = null;
        for (JsonNode v : manifest.get("versions")) {
            if (v.get("id").asText().equals(req.getMcVersion())) {
                String versionUrl = v.get("url").asText();
                String versionJson = webClient.get().uri(versionUrl).retrieve().bodyToMono(String.class).block();
                versionInfo = objectMapper.readTree(versionJson);
                break;
            }
        }

        if (versionInfo == null) {
            throw new IllegalArgumentException("Version not found: " + req.getMcVersion());
        }

        String serverHash = versionInfo.get("downloads").get("server").get("sha1").asText();
        String serverUrl = type.getDownloadTemplates().get("jar").replace("{hash}", serverHash);
        String serverSize = versionInfo.get("downloads").get("server").get("size").asText();

        Path jarPath = dir.resolve("server.jar");
        downloadFile(serverUrl, jarPath);
        return jarPath;
    }

    private Path downloadPaper(DownloadRequest req, ServerType type, Path dir) throws Exception {
        String template = type.getDownloadTemplates().get("versions")
                .replace("{mcVersion}", req.getMcVersion());
        String versionsJson = webClient.get().uri(template).retrieve().bodyToMono(String.class).block();
        JsonNode versions = objectMapper.readTree(versionsJson);

        // Get latest build
        JsonNode builds = versions.get("builds");
        int latestBuild = builds.get(builds.size() - 1).asInt();

        String buildUrl = type.getDownloadTemplates().get("build")
                .replace("{mcVersion}", req.getMcVersion())
                .replace("{build}", String.valueOf(latestBuild));
        String buildJson = webClient.get().uri(buildUrl).retrieve().bodyToMono(String.class).block();
        JsonNode build = objectMapper.readTree(buildJson);

        String filename = build.get("downloads").get("application").get("name").asText();
        String downloadUrl = type.getDownloadTemplates().get("download")
                .replace("{mcVersion}", req.getMcVersion())
                .replace("{build}", String.valueOf(latestBuild))
                .replace("{filename}", filename);

        Path jarPath = dir.resolve(filename);
        downloadFile(downloadUrl, jarPath);
        return jarPath;
    }

    private Path downloadPurpur(DownloadRequest req, ServerType type, Path dir) throws Exception {
        String downloadUrl = type.getDownloadTemplates().get("download")
                .replace("{mcVersion}", req.getMcVersion());
        Path jarPath = dir.resolve("purpur.jar");
        downloadFile(downloadUrl, jarPath);
        return jarPath;
    }

    private Path downloadFolia(DownloadRequest req, ServerType type, Path dir) throws Exception {
        // Same API as Paper, just project=folia
        String versionsUrl = type.getDownloadTemplates().get("versions")
                .replace("{mcVersion}", req.getMcVersion());
        String versionsJson = webClient.get().uri(versionsUrl).retrieve().bodyToMono(String.class).block();
        JsonNode versions = objectMapper.readTree(versionsJson);

        JsonNode builds = versions.get("builds");
        int latestBuild = builds.get(builds.size() - 1).asInt();

        String buildUrl = type.getDownloadTemplates().get("build")
                .replace("{mcVersion}", req.getMcVersion())
                .replace("{build}", String.valueOf(latestBuild));
        String buildJson = webClient.get().uri(buildUrl).retrieve().bodyToMono(String.class).block();
        JsonNode build = objectMapper.readTree(buildJson);

        String filename = build.get("downloads").get("application").get("name").asText();
        String downloadUrl = type.getDownloadTemplates().get("download")
                .replace("{mcVersion}", req.getMcVersion())
                .replace("{build}", String.valueOf(latestBuild))
                .replace("{filename}", filename);

        Path jarPath = dir.resolve(filename);
        downloadFile(downloadUrl, jarPath);
        return jarPath;
    }

    private Path downloadFabric(DownloadRequest req, ServerType type, Path dir) throws Exception {
        // Get latest loader version
        String loaderUrl = type.getDownloadTemplates().get("loaderVersions");
        String loaderJson = webClient.get().uri(loaderUrl).retrieve().bodyToMono(String.class).block();
        JsonNode loaderVersions = objectMapper.readTree(loaderJson);
        String latestLoader = loaderVersions.get(0).get("version").asText();

        // Get latest installer version
        JsonNode lastInstaller = null;
        for (JsonNode lv : loaderVersions) {
            if (lv.get("version").asText().equals(latestLoader) && lastInstaller == null) {
                lastInstaller = lv;
            }
        }

        // Get server profile
        String profileUrl = type.getDownloadTemplates().get("profile")
                .replace("{game}", req.getMcVersion())
                .replace("{loader}", latestLoader);
        String profileJson = webClient.get().uri(profileUrl).retrieve().bodyToMono(String.class).block();
        JsonNode profile = objectMapper.readTree(profileJson);

        // Download fabric server launcher jar
        String installerVer = profile.get("mainClass").asText().contains("installer") ?
                profile.get("libraries").get(0).get("name").asText().split(":")[2] : latestLoader;

        String downloadUrl = type.getDownloadTemplates().get("installer")
                .replace("{game}", req.getMcVersion())
                .replace("{loader}", latestLoader)
                .replace("{installer}", installerVer);

        Path jarPath = dir.resolve("fabric-server.jar");
        downloadFile(downloadUrl, jarPath);
        return jarPath;
    }

    private Path downloadQuilt(DownloadRequest req, ServerType type, Path dir) throws Exception {
        String loaderUrl = type.getDownloadTemplates().get("loaderVersions");
        String loaderString = webClient.get().uri(loaderUrl).retrieve().bodyToMono(String.class).block();
        // Quilt API returns array of version objects
        JsonNode loaderVersions = objectMapper.readTree(loaderString);
        String latestLoader = loaderVersions.get(0).get("version").asText();

        String profileUrl = type.getDownloadTemplates().get("profile")
                .replace("{game}", req.getMcVersion())
                .replace("{loader}", latestLoader);
        String profileJson = webClient.get().uri(profileUrl).retrieve().bodyToMono(String.class).block();

        // Quilt profile contains the launch JAR path
        Path jarPath = dir.resolve("quilt-server.jar");
        // Save the profile for the launcher to use
        Files.writeString(dir.resolve("quilt-installer.json"), profileJson);
        return jarPath;
    }

    private Path downloadForge(DownloadRequest req, ServerType type, Path dir) throws Exception {
        // Download forge installer
        String installerUrl = type.getDownloadTemplates().get("installer")
                .replace("{ver}", req.getMcVersion() + "-" + req.getLoader());
        Path installerPath = dir.resolve("forge-installer.jar");
        downloadFile(installerUrl, installerPath);

        // Run installer --installServer
        ProcessBuilder pb = new ProcessBuilder(
                "java", "-jar", installerPath.toString(), "--installServer"
        );
        pb.directory(dir.toFile());
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Forge installer failed with exit code: " + exitCode);
        }

        // Find the forge server jar
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(".jar") &&
                            p.getFileName().toString().contains("forge"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Forge server jar not found"));
        }
    }

    private Path downloadNeoForge(DownloadRequest req, ServerType type, Path dir) throws Exception {
        // Similar to Forge, use NeoForge installer
        String installerUrl = type.getDownloadTemplates().get("installer")
                .replace("{ver}", req.getMcVersion() + "-" + req.getLoader());
        Path installerPath = dir.resolve("neoforge-installer.jar");
        downloadFile(installerUrl, installerPath);

        ProcessBuilder pb = new ProcessBuilder(
                "java", "-jar", installerPath.toString(), "--installServer"
        );
        pb.directory(dir.toFile());
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("NeoForge installer failed with exit code: " + exitCode);
        }

        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(".jar") &&
                            p.getFileName().toString().contains("neoforge"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("NeoForge server jar not found"));
        }
    }

    private Path downloadVelocity(DownloadRequest req, ServerType type, Path dir) throws Exception {
        // Velocity uses Paper API
        String versionsUrl = type.getDownloadTemplates().get("versions")
                .replace("{mcVersion}", req.getMcVersion());
        String versionsJson = webClient.get().uri(versionsUrl).retrieve().bodyToMono(String.class).block();
        JsonNode versions = objectMapper.readTree(versionsJson);

        JsonNode builds = versions.get("builds");
        int latestBuild = builds.get(builds.size() - 1).asInt();

        String buildUrl = type.getDownloadTemplates().get("build")
                .replace("{mcVersion}", req.getMcVersion())
                .replace("{build}", String.valueOf(latestBuild));
        String buildJson = webClient.get().uri(buildUrl).retrieve().bodyToMono(String.class).block();
        JsonNode build = objectMapper.readTree(buildJson);

        String filename = build.get("downloads").get("application").get("name").asText();
        String downloadUrl = type.getDownloadTemplates().get("download")
                .replace("{mcVersion}", req.getMcVersion())
                .replace("{build}", String.valueOf(latestBuild))
                .replace("{filename}", filename);

        Path jarPath = dir.resolve(filename);
        downloadFile(downloadUrl, jarPath);
        return jarPath;
    }

    private void downloadFile(String url, Path dest) throws IOException {
        log.info("Downloading: {} -> {}", url, dest);
        URL downloadUrl = new URL(url);
        try (ReadableByteChannel channel = Channels.newChannel(downloadUrl.openStream());
             FileOutputStream fos = new FileOutputStream(dest.toFile())) {
            fos.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
        }
        log.info("Downloaded: {} ({} bytes)", dest.getFileName(), Files.size(dest));
    }

    public String getProgress(String downloadId) {
        return progressMap.getOrDefault(downloadId, "NOT_FOUND");
    }
}
