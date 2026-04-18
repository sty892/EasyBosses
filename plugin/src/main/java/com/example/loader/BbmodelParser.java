package com.example.loader;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

public class BbmodelParser {
    private static final Gson GSON = new Gson();

    public static ParsedBbmodel parse(String jsonContent) {
        JsonObject root = GSON.fromJson(jsonContent, JsonObject.class);

        // 1. Texture
        byte[] textureBytes = new byte[0];
        if (root.has("textures")) {
            JsonArray textures = root.getAsJsonArray("textures");
            if (!textures.isEmpty()) {
                JsonObject tex = textures.get(0).getAsJsonObject();
                if (tex.has("source")) {
                    String source = tex.get("source").getAsString();
                    String prefix = "data:image/png;base64,";
                    if (source.startsWith(prefix)) {
                        String base64 = source.substring(prefix.length());
                        textureBytes = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
        }

        // 2. Elements (Hitboxes)
        Map<String, HitboxElement> hitboxElements = new HashMap<>(); // uuid -> element
        if (root.has("elements")) {
            for (JsonElement el : root.getAsJsonArray("elements")) {
                JsonObject element = el.getAsJsonObject();
                String name = element.has("name") ? element.get("name").getAsString() : "";
                if (name.startsWith("hitbox:")) {
                    String zone = name.substring(7);
                    String uuid = element.get("uuid").getAsString();
                    
                    JsonArray fromArr = element.getAsJsonArray("from");
                    double[] from = new double[]{
                        fromArr.get(0).getAsDouble() / 16.0,
                        fromArr.get(1).getAsDouble() / 16.0,
                        fromArr.get(2).getAsDouble() / 16.0
                    };

                    JsonArray toArr = element.getAsJsonArray("to");
                    double[] to = new double[]{
                        toArr.get(0).getAsDouble() / 16.0,
                        toArr.get(1).getAsDouble() / 16.0,
                        toArr.get(2).getAsDouble() / 16.0
                    };

                    double[] origin = new double[]{0, 0, 0};
                    if (element.has("origin")) {
                        JsonArray originArr = element.getAsJsonArray("origin");
                        origin[0] = originArr.get(0).getAsDouble() / 16.0;
                        origin[1] = originArr.get(1).getAsDouble() / 16.0;
                        origin[2] = originArr.get(2).getAsDouble() / 16.0;
                    }

                    double width = to[0] - from[0];
                    double height = to[1] - from[1];
                    double depth = to[2] - from[2];
                    
                    // Center in local space: (from+to)/2
                    double[] center = new double[]{
                        (from[0] + to[0]) / 2.0,
                        (from[1] + to[1]) / 2.0,
                        (from[2] + to[2]) / 2.0
                    };

                    HitboxElement hb = new HitboxElement(zone, uuid, width, height, depth, center);
                    hitboxElements.put(uuid, hb);
                }
            }
        }

        // 3. Outliner (Bone Hierarchy)
        Map<String, BoneNode> bonesByUuid = new HashMap<>();
        List<BoneNode> rootBones = new ArrayList<>();

        if (root.has("outliner")) {
            for (JsonElement el : root.getAsJsonArray("outliner")) {
                if (el.isJsonObject()) {
                    BoneNode node = parseBoneNode(el.getAsJsonObject(), bonesByUuid);
                    rootBones.add(node);
                }
            }
        }

        // Find parent bone for each hitbox
        for (HitboxElement hb : hitboxElements.values()) {
            BoneNode parent = findParentBone(rootBones, hb.uuid);
            if (parent != null) {
                hb.parentBoneName = parent.name;
            }
        }

        // 4. Animations
        JsonArray animationsArray = root.has("animations") ? root.getAsJsonArray("animations") : new JsonArray();

        return new ParsedBbmodel(textureBytes, hitboxElements, rootBones, bonesByUuid, animationsArray, root);
    }

    private static BoneNode parseBoneNode(JsonObject obj, Map<String, BoneNode> bonesByUuid) {
        String name = obj.has("name") ? obj.get("name").getAsString() : "unknown";
        String uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : UUID.randomUUID().toString();
        
        double[] origin = new double[]{0, 0, 0};
        if (obj.has("origin")) {
            JsonArray originArr = obj.getAsJsonArray("origin");
            origin[0] = originArr.get(0).getAsDouble() / 16.0;
            origin[1] = originArr.get(1).getAsDouble() / 16.0;
            origin[2] = originArr.get(2).getAsDouble() / 16.0;
        }

        BoneNode node = new BoneNode(name, uuid, origin);
        bonesByUuid.put(uuid, node);

        if (obj.has("children")) {
            for (JsonElement childEl : obj.getAsJsonArray("children")) {
                if (childEl.isJsonPrimitive()) {
                    node.childElementUUIDs.add(childEl.getAsString());
                } else if (childEl.isJsonObject()) {
                    BoneNode childBone = parseBoneNode(childEl.getAsJsonObject(), bonesByUuid);
                    node.childBoneUUIDs.add(childBone.uuid);
                    node.children.add(childBone);
                }
            }
        }

        return node;
    }

    private static BoneNode findParentBone(List<BoneNode> bones, String elementUuid) {
        for (BoneNode bone : bones) {
            if (bone.childElementUUIDs.contains(elementUuid)) {
                return bone;
            }
            BoneNode found = findParentBone(bone.children, elementUuid);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static class HitboxElement {
        public String zone;
        public String uuid;
        public double width, height, depth;
        public double[] center; // local center relative to world origin, actually absolute in blockbench space, wait, prompt says: localCenter = (from+to)/2 - it's absolute in blockbench space! "center = origin + (from+to)/2 - origin" is mathematically just (from+to)/2. We keep it as (from+to)/2.
        public String parentBoneName;

        public HitboxElement(String zone, String uuid, double width, double height, double depth, double[] center) {
            this.zone = zone;
            this.uuid = uuid;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.center = center;
        }
    }

    public static class BoneNode {
        public String name;
        public String uuid;
        public double[] origin; // in blocks
        public List<String> childBoneUUIDs = new ArrayList<>();
        public List<String> childElementUUIDs = new ArrayList<>();
        public List<BoneNode> children = new ArrayList<>();

        public BoneNode(String name, String uuid, double[] origin) {
            this.name = name;
            this.uuid = uuid;
            this.origin = origin;
        }
    }

    public static class ParsedBbmodel {
        public byte[] textureBytes;
        public Map<String, HitboxElement> hitboxElements;
        public List<BoneNode> rootBones;
        public Map<String, BoneNode> bonesByUuid;
        public JsonArray animationsJson;
        public JsonObject rawRoot;

        public ParsedBbmodel(byte[] textureBytes, Map<String, HitboxElement> hitboxElements, List<BoneNode> rootBones, Map<String, BoneNode> bonesByUuid, JsonArray animationsJson, JsonObject rawRoot) {
            this.textureBytes = textureBytes;
            this.hitboxElements = hitboxElements;
            this.rootBones = rootBones;
            this.bonesByUuid = bonesByUuid;
            this.animationsJson = animationsJson;
            this.rawRoot = rawRoot;
        }
    }
}
