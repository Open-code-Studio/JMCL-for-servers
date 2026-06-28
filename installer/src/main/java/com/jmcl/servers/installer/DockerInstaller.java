package com.jmcl.servers.installer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Handles Docker and Docker Compose installation per platform.
 */
public class DockerInstaller {

    private final OsDetector os;

    public DockerInstaller(OsDetector os) {
        this.os = os;
    }

    public boolean isDockerInstalled() {
        try {
            Process proc = new ProcessBuilder("docker", "--version").start();
            boolean success = proc.waitFor(10, TimeUnit.SECONDS);
            return success && proc.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isComposeInstalled() {
        try {
            // Docker Compose v2
            Process proc = new ProcessBuilder("docker", "compose", "version").start();
            boolean success = proc.waitFor(10, TimeUnit.SECONDS);
            if (success && proc.exitValue() == 0) return true;

            // Try legacy compose
            Process procV1 = new ProcessBuilder("docker-compose", "--version").start();
            boolean v1Success = procV1.waitFor(10, TimeUnit.SECONDS);
            return v1Success && procV1.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void install() throws Exception {
        System.out.println("Installing Docker...");

        if (os.isMac()) {
            installMac();
        } else if (os.isLinux()) {
            installLinux();
        } else if (os.isWindows()) {
            installWindows();
        } else {
            throw new UnsupportedOperationException(
                    "Automatic Docker installation is not supported on: " + os.getName());
        }

        // Verify installation
        if (!isDockerInstalled()) {
            throw new RuntimeException("Docker installation failed. Please install manually.");
        }

        System.out.println("Docker installed successfully!");

        // Start Docker daemon if needed (Linux)
        if (os.isLinux()) {
            startDockerDaemon();
        }

        // Check compose
        if (!isComposeInstalled()) {
            System.out.println("Note: Docker Compose v2 should be included with Docker Desktop.");
            System.out.println("If not available, install with: docker-compose-plugin");
        }
    }

    private void installLinux() throws Exception {
        String pkgManager = os.getPackageManager();
        System.out.println("Using package manager: " + pkgManager);

        // Use official Docker install script for reliability
        System.out.println("Downloading official Docker install script...");
        List<String> cmd = new ArrayList<>();
        cmd.add("curl");
        cmd.add("-fsSL");
        cmd.add("https://get.docker.com");
        cmd.add("-o");
        cmd.add("/tmp/get-docker.sh");

        runCommand(cmd);

        // Run install script
        System.out.println("Running Docker install script (requires sudo)...");
        cmd.clear();
        cmd.add("sudo");
        cmd.add("sh");
        cmd.add("/tmp/get-docker.sh");
        runCommand(cmd);

        // Add current user to docker group
        String currentUser = System.getProperty("user.name");
        System.out.println("Adding user '" + currentUser + "' to docker group...");
        cmd.clear();
        cmd.add("sudo");
        cmd.add("usermod");
        cmd.add("-aG");
        cmd.add("docker");
        cmd.add(currentUser);
        runCommand(cmd);

        // Install docker-compose plugin
        System.out.println("Installing docker-compose plugin...");
        String arch = os.getArch().contains("64") ? "x86_64" : "aarch64";
        cmd.clear();
        cmd.add("sudo");
        cmd.add("sh");
        cmd.add("-c");
        cmd.add("curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-" +
                os.getArch() + " -o /usr/local/bin/docker-compose && chmod +x /usr/local/bin/docker-compose");
        try {
            runCommand(cmd);
        } catch (Exception e) {
            System.out.println("  Note: docker-compose standalone installation failed, but docker compose plugin may work.");
        }
    }

    private void installMac() throws Exception {
        System.out.println("On macOS, Docker Desktop is recommended.");
        System.out.println("Checking if Homebrew is installed...");

        boolean hasBrew = false;
        try {
            Process brewCheck = new ProcessBuilder("brew", "--version").start();
            hasBrew = brewCheck.waitFor(5, TimeUnit.SECONDS) && brewCheck.exitValue() == 0;
        } catch (Exception ignored) {}

        if (hasBrew) {
            System.out.println("Installing Docker via Homebrew...");
            List<String> cmd = new ArrayList<>();
            cmd.add("brew");
            cmd.add("install");
            cmd.add("--cask");
            cmd.add("docker");
            runCommand(cmd);

            System.out.println("Starting Docker Desktop...");
            cmd.clear();
            cmd.add("open");
            cmd.add("-a");
            cmd.add("Docker");
            runCommand(cmd);

            System.out.println("Waiting for Docker to start (30 seconds)...");
            Thread.sleep(30000);
        } else {
            System.out.println("Homebrew not found.");
            System.out.println("Please install Docker Desktop manually from:");
            System.out.println("  https://www.docker.com/products/docker-desktop/");
            System.out.println("After installation, re-run this installer.");
            System.exit(1);
        }
    }

    private void installWindows() {
        System.out.println("On Windows, please install Docker Desktop manually:");
        System.out.println("  1. Download from: https://www.docker.com/products/docker-desktop/");
        System.out.println("  2. Install and restart your computer");
        System.out.println("  3. Ensure WSL2 backend is enabled in Docker settings");
        System.out.println("  4. Re-run this installer");
        System.exit(1);
    }

    private void startDockerDaemon() throws Exception {
        if (os.isWsl()) {
            System.out.println("Running in WSL2. Ensure Docker Desktop is running on Windows.");
            System.out.println("Starting Docker daemon in WSL...");
            try {
                List<String> cmd = new ArrayList<>();
                cmd.add("sudo");
                cmd.add("service");
                cmd.add("docker");
                cmd.add("start");
                runCommand(cmd);
            } catch (Exception e) {
                System.out.println("  Note: Could not start Docker daemon. Make sure Docker Desktop is running.");
            }
        } else {
            System.out.println("Starting Docker daemon...");
            try {
                List<String> cmd = new ArrayList<>();
                cmd.add("sudo");
                cmd.add("systemctl");
                cmd.add("start");
                cmd.add("docker");
                runCommand(cmd);

                cmd.clear();
                cmd.add("sudo");
                cmd.add("systemctl");
                cmd.add("enable");
                cmd.add("docker");
                runCommand(cmd);
            } catch (Exception e) {
                System.out.println("  Note: Could not start Docker daemon via systemctl.");
            }
        }
    }

    private void runCommand(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Command exited with code " + exitCode + ": " +
                    String.join(" ", command));
        }
    }
}
