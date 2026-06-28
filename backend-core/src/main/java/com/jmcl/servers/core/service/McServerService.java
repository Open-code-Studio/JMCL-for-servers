package com.jmcl.servers.core.service;

import com.jmcl.servers.core.model.LogEntry;
import com.jmcl.servers.core.model.ServerInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class McServerService {

    private static final Logger log = LoggerFactory.getLogger(McServerService.class);

    private final ServerInstanceManager instanceManager;
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();
    private final Map<String, Thread> outputReaders = new ConcurrentHashMap<>();
    private final Map<String, List<LogEntry>> logBuffers = new ConcurrentHashMap<>();
    private final AtomicBoolean acceptingInput = new AtomicBoolean(false);

    @Value("${jmcl.java.home:/usr/lib/jvm/java-21-openjdk}")
    private String javaHome;

    public McServerService(ServerInstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    public synchronized void start(String serverId) throws Exception {
        ServerInstance instance = instanceManager.getById(serverId)
                .orElseThrow(() -> new NoSuchElementException("Server not found: " + serverId));

        if (instance.getStatus() == ServerInstance.ServerStatus.RUNNING) {
            throw new IllegalStateException("Server is already running: " + serverId);
        }

        Path serverDir = Path.of(instance.getDataDir());
        Path jarPath = Path.of(instance.getServerJar());

        if (!Files.exists(jarPath)) {
            throw new FileNotFoundException("Server jar not found: " + jarPath);
        }

        // Check eula
        Path eulaPath = serverDir.resolve("eula.txt");
        if (!Files.exists(eulaPath) || !Files.readString(eulaPath).contains("eula=true")) {
            Files.writeString(eulaPath, "eula=true\n");
        }

        instanceManager.updateStatus(serverId, ServerInstance.ServerStatus.STARTING);

        List<String> command = buildStartCommand(instance, jarPath);
        log.info("Starting server {}: {}", serverId, String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(serverDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        runningProcesses.put(serverId, process);
        logBuffers.put(serverId, new ArrayList<>());

        // Read output in background thread
        Thread readerThread = new Thread(() -> readOutput(serverId, process));
        readerThread.setDaemon(true);
        readerThread.setName("mc-server-" + serverId);
        readerThread.start();
        outputReaders.put(serverId, readerThread);

        // Wait briefly to check if process dies immediately
        Thread.sleep(2000);
        if (!process.isAlive()) {
            int exitCode = process.exitValue();
            runningProcesses.remove(serverId);
            instanceManager.updateStatus(serverId, ServerInstance.ServerStatus.ERROR);
            throw new RuntimeException("Server process died immediately with exit code: " + exitCode +
                    ". Check logs for details.");
        }

        instance.setPid(process.pid());
        instanceManager.updateStatus(serverId, ServerInstance.ServerStatus.RUNNING);
        log.info("Server {} started with PID {}", serverId, process.pid());
    }

    private List<String> buildStartCommand(ServerInstance instance, Path jarPath) {
        List<String> cmd = new ArrayList<>();
        cmd.add(javaHome + "/bin/java");

        // Memory settings
        cmd.add("-Xms" + instance.getMinRam() + "M");
        cmd.add("-Xmx" + instance.getMaxRam() + "M");

        // Additional JVM args
        cmd.add("-XX:+UseG1GC");
        cmd.add("-XX:+ParallelRefProcEnabled");
        cmd.add("-XX:MaxGCPauseMillis=200");
        cmd.add("-XX:+UnlockExperimentalVMOptions");
        cmd.add("-XX:+DisableExplicitGC");
        cmd.add("-XX:+AlwaysPreTouch");
        cmd.add("-XX:G1NewSizePercent=30");
        cmd.add("-XX:G1MaxNewSizePercent=40");
        cmd.add("-XX:G1HeapRegionSize=8M");
        cmd.add("-XX:G1ReservePercent=20");
        cmd.add("-XX:G1HeapWastePercent=5");
        cmd.add("-XX:G1MixedGCCountTarget=4");
        cmd.add("-XX:InitiatingHeapOccupancyPercent=15");
        cmd.add("-XX:G1MixedGCLiveThresholdPercent=90");
        cmd.add("-XX:G1RSetUpdatingPauseTimePercent=5");
        cmd.add("-XX:SurvivorRatio=32");
        cmd.add("-XX:+PerfDisableSharedMem");
        cmd.add("-XX:MaxTenuringThreshold=1");

        // Use aikar's flags for Paper/Purpur
        if (List.of("PAPER", "PURPUR", "FOLIA").contains(instance.getType())) {
            cmd.add("-Dusing.aikars.flags=minecraft");
        }

        // Custom Java args
        if (instance.getJavaArgs() != null && !instance.getJavaArgs().isEmpty()) {
            cmd.addAll(Arrays.asList(instance.getJavaArgs().split("\\s+")));
        }

        cmd.add("-jar");
        cmd.add(jarPath.toString());
        cmd.add("nogui");

        // Port override
        if (instance.getPort() != 25565) {
            cmd.add("--port");
            cmd.add(String.valueOf(instance.getPort()));
        }

        return cmd;
    }

    private void readOutput(String serverId, Process process) {
        List<LogEntry> buffer = logBuffers.get(serverId);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String level = "INFO";
                if (line.contains("ERROR") || line.contains("FATAL")) level = "ERROR";
                else if (line.contains("WARN")) level = "WARN";

                LogEntry entry = LogEntry.builder()
                        .serverId(serverId)
                        .timestamp(LocalDateTime.now().format(dtf))
                        .level(level)
                        .message(line)
                        .build();

                if (buffer != null) {
                    buffer.add(entry);
                    if (buffer.size() > 1000) buffer.remove(0);
                }
            }
        } catch (IOException e) {
            log.warn("Output reader for server {} interrupted", serverId, e);
        } finally {
            try {
                int exitCode = process.waitFor();
                log.info("Server {} exited with code {}", serverId, exitCode);
                runningProcesses.remove(serverId);
                instanceManager.updateStatus(serverId, ServerInstance.ServerStatus.STOPPED);
            } catch (Exception e) {
                log.error("Error handling process exit for {}", serverId, e);
            }
        }
    }

    public void stop(String serverId) throws Exception {
        Process process = runningProcesses.get(serverId);
        if (process == null) {
            throw new IllegalStateException("Server is not running: " + serverId);
        }

        instanceManager.updateStatus(serverId, ServerInstance.ServerStatus.STOPPING);
        log.info("Stopping server: {}", serverId);

        // Send stop command via stdin
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream()))) {
            writer.write("stop\n");
            writer.flush();
        }

        // Wait for graceful shutdown
        boolean terminated = process.waitFor(60, TimeUnit.SECONDS);
        if (!terminated) {
            log.warn("Server {} did not stop gracefully, force killing", serverId);
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }

        runningProcesses.remove(serverId);
        instanceManager.updateStatus(serverId, ServerInstance.ServerStatus.STOPPED);
        log.info("Server {} stopped", serverId);
    }

    public void restart(String serverId) throws Exception {
        if (runningProcesses.containsKey(serverId)) {
            stop(serverId);
            // Wait a bit for cleanup
            Thread.sleep(3000);
        }
        start(serverId);
    }

    public void sendCommand(String serverId, String command) throws Exception {
        Process process = runningProcesses.get(serverId);
        if (process == null) {
            throw new IllegalStateException("Server is not running: " + serverId);
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(command + "\n");
            writer.flush();
        }
    }

    public List<LogEntry> getLogs(String serverId, int tail) {
        List<LogEntry> buffer = logBuffers.get(serverId);
        if (buffer == null) return List.of();

        int size = buffer.size();
        int start = Math.max(0, size - tail);
        return new ArrayList<>(buffer.subList(start, size));
    }

    public Map<String, ServerInstance.ServerStatus> getAllStatuses() {
        Map<String, ServerInstance.ServerStatus> statuses = new HashMap<>();
        for (ServerInstance si : instanceManager.listAll()) {
            statuses.put(si.getId(), si.getStatus());
        }
        return statuses;
    }
}
