package com.jmcl.servers.installer;

/**
 * Detects operating system and provides platform-specific information.
 */
public class OsDetector {

    private final String osName;
    private final String osVersion;
    private final String arch;

    public OsDetector() {
        this.osName = System.getProperty("os.name").toLowerCase();
        this.osVersion = System.getProperty("os.version");
        this.arch = System.getProperty("os.arch").toLowerCase();
    }

    public String getName() {
        if (isWindows()) return "Windows";
        if (isMac()) return "macOS";
        if (isLinux()) return "Linux";
        return osName;
    }

    public String getVersion() {
        return osVersion;
    }

    public String getArch() {
        return arch;
    }

    public boolean isWindows() {
        return osName.contains("win");
    }

    public boolean isMac() {
        return osName.contains("mac");
    }

    public boolean isLinux() {
        return osName.contains("nix") || osName.contains("nux") || osName.contains("aix");
    }

    /**
     * Returns the appropriate package manager command for this OS.
     */
    public String getPackageManager() {
        if (isMac()) {
            return "brew";
        }
        if (isLinux()) {
            // Detect specific Linux distro
            try {
                Process proc = new ProcessBuilder("cat", "/etc/os-release")
                        .start();
                String output = new String(proc.getInputStream().readAllBytes()).toLowerCase();
                proc.waitFor();

                if (output.contains("ubuntu") || output.contains("debian")) {
                    return "apt-get";
                } else if (output.contains("centos") || output.contains("rhel") ||
                           output.contains("fedora")) {
                    return "yum/dnf";
                } else if (output.contains("arch")) {
                    return "pacman";
                } else if (output.contains("alpine")) {
                    return "apk";
                } else if (output.contains("suse") || output.contains("opensuse")) {
                    return "zypper";
                }
            } catch (Exception ignored) {}
            return "auto";
        }
        if (isWindows()) {
            return "winget";
        }
        return "unknown";
    }

    public boolean isWsl() {
        if (isLinux()) {
            try {
                Process proc = new ProcessBuilder("cat", "/proc/version")
                        .start();
                String output = new String(proc.getInputStream().readAllBytes()).toLowerCase();
                proc.waitFor();
                return output.contains("microsoft") || output.contains("wsl");
            } catch (Exception ignored) {}
        }
        return false;
    }
}
