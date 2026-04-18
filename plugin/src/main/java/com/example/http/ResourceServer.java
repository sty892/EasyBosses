package com.example.http;

import com.example.BossFrameworkPlugin;
import com.example.boss.BossDefinition;
import com.example.boss.BossRegistry;
import io.javalin.Javalin;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourceServer {
    private final BossFrameworkPlugin plugin;
    private final BossRegistry registry;
    private Javalin app;
    private String currentHash = "unknown";

    public ResourceServer(BossFrameworkPlugin plugin, BossRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void start(int port) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (BossDefinition def : registry.getAll().values()) {
                md.update(def.id.getBytes(StandardCharsets.UTF_8));
                if (def.geoJson != null) md.update(def.geoJson.getBytes(StandardCharsets.UTF_8));
                if (def.animationsJson != null) md.update(def.animationsJson.getBytes(StandardCharsets.UTF_8));
                if (def.texturePng != null) md.update(def.texturePng);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            this.currentHash = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(BossFrameworkPlugin.class.getClassLoader());

        app = Javalin.create().start(port);

        Thread.currentThread().setContextClassLoader(classLoader);

        app.get("/version", ctx -> {
            ctx.json(Map.of("hash", currentHash));
        });

        app.get("/boss/{id}/pack", ctx -> {
            String id = ctx.pathParam("id");
            BossDefinition def = registry.getBoss(id);
            if (def == null) {
                ctx.status(404).result("Not found");
                return;
            }

            byte[] zip = getBossPackZip(def);
            ctx.contentType("application/zip");
            ctx.result(zip);
        });
        
        plugin.getLogger().info("Resource server started on port " + port);
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    private byte[] getBossPackZip(BossDefinition definition) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(baos);

            if (definition.geoJson != null) {
                zip.putNextEntry(new ZipEntry("geo/" + definition.id + ".geo.json"));
                zip.write(definition.geoJson.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }

            if (definition.animationsJson != null) {
                zip.putNextEntry(new ZipEntry("animations/" + definition.id + ".animations.json"));
                zip.write(definition.animationsJson.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }

            if (definition.texturePng != null && definition.texturePng.length > 0) {
                zip.putNextEntry(new ZipEntry("textures/entity/" + definition.id + ".png"));
                zip.write(definition.texturePng);
                zip.closeEntry();
            }

            zip.close();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public String getCurrentHash() {
        return currentHash;
    }
}