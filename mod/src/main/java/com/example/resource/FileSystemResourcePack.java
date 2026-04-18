package com.example.resource;

import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

public class FileSystemResourcePack implements ResourcePack {
    private final String id;
    private final Path root;

    public FileSystemResourcePack(String id, Path root) {
        this.id = id;
        this.root = root;
    }

    @Override
    public @Nullable InputSupplier<InputStream> openRoot(String... segments) {
        Path path = root;
        for (String segment : segments) path = path.resolve(segment);
        if (Files.exists(path)) {
            Path finalPath = path;
            return () -> Files.newInputStream(finalPath);
        }
        return null;
    }

    @Override
    public @Nullable InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        if (type != ResourceType.CLIENT_RESOURCES) return null;
        try {
            if (!Files.exists(root)) return null;
            try (var streams = Files.list(root)) {
                for (Path ipDir : streams.toList()) {
                    if (!Files.isDirectory(ipDir)) continue;
                    Path path = ipDir.resolve("assets").resolve(id.getNamespace()).resolve(id.getPath());
                    if (Files.exists(path)) {
                        return InputSupplier.create(path);
                    }
                }
            }
        } catch (IOException e) {
            // Ignore
        }
        return null;
    }

    @Override
    public void listResources(ResourceType type, String namespace, String prefix, ResourceConsumer consumer) {
        if (type != ResourceType.CLIENT_RESOURCES) return;
        try {
            if (!Files.exists(root)) return;
            try (var streams = Files.list(root)) {
                for (Path ipDir : streams.toList()) {
                    if (!Files.isDirectory(ipDir)) continue;
                    Path assets = ipDir.resolve("assets").resolve(namespace);
                    if (!Files.exists(assets)) continue;
                    
                    Path searchRoot = assets.resolve(prefix);
                    if (!Files.exists(searchRoot)) continue;

                    Files.walk(searchRoot).filter(Files::isRegularFile).forEach(path -> {
                        String relativePath = assets.relativize(path).toString().replace("\\", "/");
                        Identifier id = Identifier.of(namespace, relativePath);
                        consumer.accept(id, InputSupplier.create(path));
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        return type == ResourceType.CLIENT_RESOURCES ? Collections.singleton("bossframework") : Collections.emptySet();
    }

    @Override
    public <T> @Nullable T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
        Path packMeta = root.resolve("pack.mcmeta");
        if (Files.exists(packMeta)) {
            try (InputStream is = Files.newInputStream(packMeta)) {
                return metaReader.fromJson(new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(is)).getAsJsonObject().getAsJsonObject("pack"));
            }
        }
        // Fallback for metadata if pack.mcmeta doesn't exist at root
        if (metaReader.getKey().equals("pack")) {
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("description", "Boss Framework Resources");
            obj.addProperty("pack_format", 15);
            return metaReader.fromJson(obj);
        }
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void close() {}
}
