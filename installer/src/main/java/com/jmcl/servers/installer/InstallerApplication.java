package com.jmcl.servers.installer;

import java.io.*;
import java.nio.file.*;
import java.util.Scanner;

/**
 * JMCL-for-servers Installer
 * Auto-installs Docker-based Minecraft server management platform.
 * Supports: Linux (systemd), macOS (launchd)
 */
public class InstallerApplication {

    private static final String BANNER = """
        ╔══════════════════════════════════════════╗
        ║     JMCL Server Manager - Installer      ║
        ║     v1.0.0                               ║
        ╚══════════════════════════════════════════╝
        """;

    private static final Path INSTALL_DIR = Path.of(
            System.getProperty("os.name").toLowerCase().contains("mac")
                    ? "/Library/JMCL-Servers"
                    : "/opt/jmcl-servers"
    );

    private static final Path DATA_DIR = Path.of(
            System.getProperty("os.name").toLowerCase().contains("mac")
                    ? "/Library/JMCL-Servers/data"
                    : "/var/lib/jmcl-servers"
    );

    private static boolean autoMode = false;

    public static void main(String[] args) throws Exception {
        autoMode = args.length > 0 && (args[0].equals("--auto") || args[0].equals("-y") || args[0].equals("--yes"));

        System.out.println(BANNER);
        if (autoMode) {
            System.out.println("[AUTO mode] All prompts default to YES.");
        }

        OsDetector os = new OsDetector();
        System.out.println("Detected OS: " + os.getName() + " (" + os.getArch() + ")");
        System.out.println("Package Manager: " + os.getPackageManager());
        System.out.println("Install Directory: " + INSTALL_DIR);
        System.out.println("Data Directory: " + DATA_DIR);
        System.out.println();

        Scanner scanner = autoMode ? null : new Scanner(System.in);

        // 1. Check prerequisites
        System.out.println("═══ Step 1/5: Checking Prerequisites ═══");
        checkPrerequisites(os);

        // 2. Install Docker if needed
        System.out.println("\n═══ Step 2/5: Installing Docker ═══");
        DockerInstaller dockerInstaller = new DockerInstaller(os);
        if (!dockerInstaller.isDockerInstalled()) {
            if (promptYesNo(scanner, "Docker is not installed. Install now?")) {
                dockerInstaller.install();
            } else {
                System.out.println("Docker is required. Please install it manually and re-run.");
                System.exit(1);
            }
        } else {
            System.out.println("Docker is already installed.");
        }

        // 3. Create directories and copy files
        System.out.println("\n═══ Step 3/5: Setting up JMCL Server Manager ═══");
        setupDirectories();
        copyProjectFiles();

        // 4. Configure system service
        System.out.println("\n═══ Step 4/5: Configuring Auto-start Service ═══");
        ServiceConfigurator serviceConfig = new ServiceConfigurator(os);
        if (promptYesNo(scanner, "Enable auto-start on system boot?")) {
            serviceConfig.installService();
            System.out.println("Service installed. JMCL will start automatically on boot.");
        } else {
            System.out.println("Skipped auto-start configuration.");
        }

        // 5. Start services
        System.out.println("\n═══ Step 5/5: Starting JMCL Services ═══");
        if (promptYesNo(scanner, "Start JMCL Server Manager now?")) {
            startServices();
            System.out.println("\n✅ JMCL Server Manager is now running!");
            String frontendUrl = "http://localhost:252540";
            System.out.println("   Open your browser and visit: " + frontendUrl);
        }

        System.out.println("\n═══════════════════════════════════════");
        System.out.println("Installation Complete!");
        System.out.println("───────────────────────────────────────");
        System.out.println("Frontend URL:  http://localhost:252540");
        System.out.println("Backend API:   http://localhost:252541");
        System.out.println("Install Dir:   " + INSTALL_DIR);
        System.out.println("Data Dir:      " + DATA_DIR);
        System.out.println("───────────────────────────────────────");
        System.out.println("Manual control:");
        System.out.println("  Start:   docker-compose -f " + INSTALL_DIR + "/docker-compose.yml up -d");
        System.out.println("  Stop:    docker-compose -f " + INSTALL_DIR + "/docker-compose.yml down");
        System.out.println("  Logs:    docker-compose -f " + INSTALL_DIR + "/docker-compose.yml logs -f");
        System.out.println("═══════════════════════════════════════");

        if (scanner != null) scanner.close();
    }

    private static boolean promptYesNo(Scanner scanner, String message) {
        if (autoMode) {
            System.out.println(message + " [Y/n]: y  (auto)");
            return true;
        }
        System.out.print(message + " [Y/n]: ");
        String response = scanner.nextLine().trim().toLowerCase();
        return response.isEmpty() || response.equals("y");
    }

    private static void checkPrerequisites(OsDetector os) throws Exception {
        // Check Java
        String javaVersion = System.getProperty("java.version");
        System.out.println("  Java: " + javaVersion + " ✓");

        // Check available disk space
        long freeSpace = Files.getFileStore(Path.of("/")).getUsableSpace() / (1024 * 1024 * 1024);
        System.out.println("  Free Disk: " + freeSpace + " GB");
        if (freeSpace < 5) {
            System.out.println("  ⚠ Low disk space. JMCL needs at least 5GB free.");
        }

        // Check memory
        long totalMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        System.out.println("  Available Memory: " + totalMem + " MB");

        System.out.println("  Prerequisites check complete.");
    }

    private static void setupDirectories() throws IOException {
        Files.createDirectories(INSTALL_DIR);
        Files.createDirectories(DATA_DIR);
        Files.createDirectories(DATA_DIR.resolve("instances"));
        Files.createDirectories(DATA_DIR.resolve("servers"));
        System.out.println("  Created: " + INSTALL_DIR);
        System.out.println("  Created: " + DATA_DIR);
    }

    private static void copyProjectFiles() throws IOException {
        // Find the project root (where docker-compose.yml is)
        Path projectRoot = findProjectRoot();

        if (projectRoot != null) {
            // Copy docker-compose.yml
            Path composeFile = projectRoot.resolve("docker-compose.yml");
            if (Files.exists(composeFile)) {
                Files.copy(composeFile, INSTALL_DIR.resolve("docker-compose.yml"),
                        StandardCopyOption.REPLACE_EXISTING);
                System.out.println("  Copied: docker-compose.yml");
            }

            // Copy backend-core and frontend directories
            copyDir(projectRoot.resolve("backend-core"), INSTALL_DIR.resolve("backend-core"));
            copyDir(projectRoot.resolve("frontend"), INSTALL_DIR.resolve("frontend"));

            System.out.println("  Project files copied successfully.");
        } else {
            System.out.println("  ⚠ Running from JAR, project files should be in current directory.");
            System.out.println("  Please ensure docker-compose.yml and source directories are in: " +
                    Path.of("").toAbsolutePath());
        }
    }

    private static Path findProjectRoot() {
        // First, check current working directory
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("docker-compose.yml"))) {
            return cwd;
        }

        // Check if we're running from the installer directory
        Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("docker-compose.yml"))) {
            return parent;
        }

        return null;
    }

    private static void copyDir(Path src, Path dest) throws IOException {
        if (!Files.exists(src)) return;

        Files.walk(src).forEach(source -> {
            try {
                Path target = dest.resolve(src.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                System.err.println("  Error copying: " + source + " -> " + e.getMessage());
            }
        });
    }

    private static void startServices() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "docker-compose", "-f", INSTALL_DIR.resolve("docker-compose.yml").toString(),
                "up", "-d", "--build"
        );
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            System.out.println("  ⚠ Docker Compose exited with code: " + exitCode);
            System.out.println("  You may need to build images manually:");
            System.out.println("    cd " + INSTALL_DIR + " && docker-compose up -d --build");
        }
    }
}
