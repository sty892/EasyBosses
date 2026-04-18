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
                        return () -> Files.newInputStream(path);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void listResources(ResourceType type, String namespace, String prefix, ResourceConsumer consumer) {
        // GeckoLib might need this to discover resources.
        // For simplicity, we can skip it unless GeckoLib fails to find the model.
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        return type == ResourceType.CLIENT_RESOURCES ? Collections.singleton("bossframework") : Collections.emptySet();
    }

    @Override
    public <T> @Nullable T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void close() {}
}
