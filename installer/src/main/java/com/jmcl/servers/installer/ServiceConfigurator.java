package com.jmcl.servers.installer;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/**
 * Configures system services for auto-start on boot.
 * Linux: systemd service
 * macOS: launchd plist
 */
public class ServiceConfigurator {

    private final OsDetector os;
    private final Path installDir;
    private final Path dataDir;

    public ServiceConfigurator(OsDetector os) {
        this.os = os;
        this.installDir = Path.of(
                os.isMac() ? "/Library/JMCL-Servers" : "/opt/jmcl-servers"
        );
        this.dataDir = Path.of(
                os.isMac() ? "/Library/JMCL-Servers/data" : "/var/lib/jmcl-servers"
        );
    }

    public void installService() throws Exception {
        if (os.isLinux()) {
            installSystemdService();
        } else if (os.isMac()) {
            installLaunchdService();
        } else {
            System.out.println("Auto-start setup not supported on this OS.");
            System.out.println("Please configure manually.");
        }
    }

    // ─── Linux systemd ───────────────────────────────────────

    private void installSystemdService() throws Exception {
        System.out.println("Creating systemd service...");

        Path serviceFile = Path.of("/etc/systemd/system/jmcl-server-manager.service");

        // Build the systemd unit file content
        String unitContent = """
                [Unit]
                Description=JMCL Minecraft Server Manager
                After=docker.service network-online.target
                Requires=docker.service
                Wants=network-online.target

                [Service]
                Type=oneshot
                RemainAfterExit=yes
                WorkingDirectory=%s
                ExecStart=/usr/bin/docker compose -f %s/docker-compose.yml up -d
                ExecStop=/usr/bin/docker compose -f %s/docker-compose.yml down
                ExecReload=/usr/bin/docker compose -f %s/docker-compose.yml restart
                Restart=on-failure
                RestartSec=10
                StandardOutput=journal
                StandardError=journal
                SyslogIdentifier=jmcl-server

                [Install]
                WantedBy=multi-user.target
                """.formatted(installDir, installDir, installDir, installDir);

        // Write service file (need sudo)
        System.out.println("Writing service file: " + serviceFile);
        Path tempFile = Files.createTempFile("jmcl-service", ".service");
        Files.writeString(tempFile, unitContent);

        // Copy with sudo
        runSudo("cp", tempFile.toString(), serviceFile.toString());
        Files.deleteIfExists(tempFile);

        // Set permissions
        runSudo("chmod", "644", serviceFile.toString());

        // Reload systemd
        runSudo("systemctl", "daemon-reload");

        // Enable service
        runSudo("systemctl", "enable", "jmcl-server-manager.service");

        System.out.println("Systemd service installed and enabled:");
        System.out.println("  Start:   sudo systemctl start jmcl-server-manager");
        System.out.println("  Stop:    sudo systemctl stop jmcl-server-manager");
        System.out.println("  Status:  sudo systemctl status jmcl-server-manager");
        System.out.println("  Logs:    sudo journalctl -u jmcl-server-manager -f");
    }

    // ─── macOS launchd ───────────────────────────────────────

    private void installLaunchdService() throws Exception {
        System.out.println("Creating launchd service...");

        String serviceName = "com.jmcl.server-manager";
        Path plistPath = Path.of("/Library/LaunchDaemons", serviceName + ".plist");

        // Build the launchd plist content
        String plistContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
                  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0">
                <dict>
                    <key>Label</key>
                    <string>%s</string>
                    <key>ProgramArguments</key>
                    <array>
                        <string>/usr/local/bin/docker</string>
                        <string>compose</string>
                        <string>-f</string>
                        <string>%s/docker-compose.yml</string>
                        <string>up</string>
                    </array>
                    <key>RunAtLoad</key>
                    <true/>
                    <key>KeepAlive</key>
                    <true/>
                    <key>WorkingDirectory</key>
                    <string>%s</string>
                    <key>StandardOutPath</key>
                    <string>%s/jmcl-stdout.log</string>
                    <key>StandardErrorPath</key>
                    <string>%s/jmcl-stderr.log</string>
                    <key>EnvironmentVariables</key>
                    <dict>
                        <key>PATH</key>
                        <string>/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin</string>
                    </dict>
                </dict>
                </plist>
                """.formatted(serviceName, installDir, installDir, dataDir, dataDir);

        // Write plist file (need sudo)
        Path tempFile = Files.createTempFile("jmcl-plist", ".plist");
        Files.writeString(tempFile, plistContent);

        runSudo("cp", tempFile.toString(), plistPath.toString());
        Files.deleteIfExists(tempFile);

        // Set permissions
        runSudo("chmod", "644", plistPath.toString());
        runSudo("chown", "root:wheel", plistPath.toString());

        // Load service
        runSudo("launchctl", "load", "-w", plistPath.toString());

        System.out.println("Launchd service installed and loaded:");
        System.out.println("  Start:   sudo launchctl load -w " + plistPath);
        System.out.println("  Stop:    sudo launchctl unload " + plistPath);
        System.out.println("  Status:  sudo launchctl list " + serviceName);
    }

    // ─── Helpers ─────────────────────────────────────────────

    private void runSudo(String... command) throws Exception {
        // Try without sudo first (if already root)
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process proc = pb.start();
            int exit = proc.waitFor();
            if (exit == 0) return;
        } catch (Exception ignored) {}

        // Run with sudo
        String[] sudoCmd = new String[command.length + 1];
        sudoCmd[0] = "sudo";
        System.arraycopy(command, 0, sudoCmd, 1, command.length);

        System.out.println("  Executing: sudo " + String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(sudoCmd);
        pb.inheritIO();
        Process proc = pb.start();
        int exit = proc.waitFor();

        if (exit != 0) {
            System.err.println("  Warning: Command exited with code " + exit + ": sudo " +
                    String.join(" ", command));
            System.err.println("  You may need to run this installer with sudo.");
        }
    }
}
