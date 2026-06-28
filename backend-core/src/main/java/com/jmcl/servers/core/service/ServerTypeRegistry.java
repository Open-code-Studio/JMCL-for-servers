package com.jmcl.servers.core.service;

import com.jmcl.servers.core.model.ServerType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ServerTypeRegistry {

    private final Map<String, ServerType> registry = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        register(ServerType.builder()
                .id("VANILLA")
                .name("Mojang Vanilla Server")
                .category("VANILLA")
                .description("Official Minecraft server from Mojang")
                .apiBase("https://piston-data.mojang.com")
                .downloadTemplates(Map.of(
                        "jar", "https://piston-data.mojang.com/v1/objects/{hash}/server.jar",
                        "manifest", "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
                ))
                .build());

        register(ServerType.builder()
                .id("PAPER")
                .name("PaperMC")
                .category("HYBRID")
                .description("High-performance Spigot fork with plugin support")
                .apiBase("https://papermc.io/api/v2")
                .downloadTemplates(Map.of(
                        "projects", "https://papermc.io/api/v2/projects",
                        "versions", "https://papermc.io/api/v2/projects/paper/versions/{mcVersion}",
                        "build", "https://papermc.io/api/v2/projects/paper/versions/{mcVersion}/builds/{build}",
                        "download", "https://papermc.io/api/v2/projects/paper/versions/{mcVersion}/builds/{build}/downloads/{filename}"
                ))
                .build());

        register(ServerType.builder()
                .id("PURPUR")
                .name("PurpurMC")
                .category("HYBRID")
                .description("Paper fork with more configuration options")
                .apiBase("https://api.purpurmc.org/v2")
                .downloadTemplates(Map.of(
                        "versions", "https://api.purpurmc.org/v2/purpur/{mcVersion}",
                        "latest", "https://api.purpurmc.org/v2/purpur/{mcVersion}/latest",
                        "download", "https://api.purpurmc.org/v2/purpur/{mcVersion}/latest/download"
                ))
                .build());

        register(ServerType.builder()
                .id("FOLIA")
                .name("Folia")
                .category("HYBRID")
                .description("Paper fork with multithreaded region-based ticking")
                .apiBase("https://papermc.io/api/v2")
                .downloadTemplates(Map.of(
                        "versions", "https://papermc.io/api/v2/projects/folia/versions/{mcVersion}",
                        "build", "https://papermc.io/api/v2/projects/folia/versions/{mcVersion}/builds/{build}",
                        "download", "https://papermc.io/api/v2/projects/folia/versions/{mcVersion}/builds/{build}/downloads/{filename}"
                ))
                .build());

        register(ServerType.builder()
                .id("FABRIC")
                .name("Fabric Server")
                .category("MODDED")
                .description("Lightweight modding platform")
                .apiBase("https://meta.fabricmc.net/v2")
                .downloadTemplates(Map.of(
                        "loaderVersions", "https://meta.fabricmc.net/v2/versions/loader",
                        "gameVersions", "https://meta.fabricmc.net/v2/versions/game",
                        "profile", "https://meta.fabricmc.net/v2/versions/loader/{game}/{loader}/profile/server",
                        "installer", "https://meta.fabricmc.net/v2/versions/loader/{game}/{loader}/{installer}/server/jar"
                ))
                .build());

        register(ServerType.builder()
                .id("QUILT")
                .name("Quilt Server")
                .category("MODDED")
                .description("Open, community-driven modding platform")
                .apiBase("https://meta.quiltmc.org/v3")
                .downloadTemplates(Map.of(
                        "loaderVersions", "https://meta.quiltmc.org/v3/versions/loader",
                        "gameVersions", "https://meta.quiltmc.org/v3/versions/game",
                        "profile", "https://meta.quiltmc.org/v3/versions/loader/{game}/{loader}/profile/server"
                ))
                .build());

        register(ServerType.builder()
                .id("FORGE")
                .name("Minecraft Forge Server")
                .category("MODDED")
                .description("Classic modding platform")
                .apiBase("https://maven.minecraftforge.net")
                .downloadTemplates(Map.of(
                        "versions", "https://jmcl.glavo.site/metadata/forge/",
                        "installer", "https://maven.minecraftforge.net/net/minecraftforge/forge/{ver}/forge-{ver}-installer.jar",
                        "mirror", "https://bmclapi2.bangbang93.com/forge/download?mcversion={mc}&version={forge}"
                ))
                .build());

        register(ServerType.builder()
                .id("NEOFORGE")
                .name("NeoForge Server")
                .category("MODDED")
                .description("Modern fork of Forge")
                .apiBase("https://maven.neoforged.net")
                .downloadTemplates(Map.of(
                        "versions", "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge",
                        "installer", "https://maven.neoforged.net/releases/net/neoforged/neoforge/{ver}/neoforge-{ver}-installer.jar",
                        "mirror", "https://bmclapi2.bangbang93.com/neoforge/version/{ver}/download/installer.jar"
                ))
                .build());

        register(ServerType.builder()
                .id("VELOCITY")
                .name("Velocity")
                .category("PROXY")
                .description("Modern high-performance proxy server")
                .apiBase("https://papermc.io/api/v2")
                .downloadTemplates(Map.of(
                        "versions", "https://papermc.io/api/v2/projects/velocity/versions/{mcVersion}",
                        "build", "https://papermc.io/api/v2/projects/velocity/versions/{mcVersion}/builds/{build}",
                        "download", "https://papermc.io/api/v2/projects/velocity/versions/{mcVersion}/builds/{build}/downloads/{filename}"
                ))
                .build());
    }

    private void register(ServerType type) {
        registry.put(type.getId(), type);
    }

    public List<ServerType> getAll() {
        return new ArrayList<>(registry.values());
    }

    public Optional<ServerType> getById(String id) {
        return Optional.ofNullable(registry.get(id.toUpperCase()));
    }

    public List<ServerType> getByCategory(String category) {
        return registry.values().stream()
                .filter(t -> t.getCategory().equalsIgnoreCase(category))
                .toList();
    }
}
