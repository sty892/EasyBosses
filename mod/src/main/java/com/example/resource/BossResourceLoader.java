package com.example.resource;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BossResourceLoader {
    private static final Gson GSON = new Gson();

    public static void loadFromServer(final String rawIp, final int port) {
        CompletableFuture.runAsync(() -> {
            try {
                String ip = rawIp;
                if (ip == null || ip.isEmpty() || ip.equals("local")) {
                    ip = "127.0.0.1";
                }
                // Use only host without port
                final String host = ip.split(":")[0];

                URL listUrl = new URI("http://" + host + ":" + port + "/api/bosses").toURL();
                List<String> bossIds;
                try (InputStreamReader reader = new InputStreamReader(listUrl.openStream())) {
                    bossIds = GSON.fromJson(reader, new TypeToken<List<String>>(){}.getType());
                }

                if (bossIds == null) return;

                Path cacheDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("bossframework_cache").resolve(host);
                if (!Files.exists(cacheDir)) {
                    Files.createDirectories(cacheDir);
                }

                for (String bossId : bossIds) {
                    URL url = new URI("http://" + host + ":" + port + "/boss/" + bossId + "/pack").toURL();
                    try (InputStream in = url.openStream();
                         ZipInputStream zis = new ZipInputStream(in)) {
                        ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null) {
                            if (entry.isDirectory()) continue;

                            Path dest = cacheDir.resolve(entry.getName());
                            Files.createDirectories(dest.getParent());
                            Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    System.out.println("Boss resources loaded for " + bossId);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}