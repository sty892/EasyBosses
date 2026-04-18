package com.example.loader;

import com.example.hitbox.HitboxTrack;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;

public class HitboxTrackBuilder {

    public static Map<String, HitboxTrack> buildTracks(BbmodelParser.ParsedBbmodel model) {
        Map<String, HitboxTrack> tracks = new HashMap<>();

        for (JsonElement animEl : model.animationsJson) {
            JsonObject animObj = animEl.getAsJsonObject();
            String animName = animObj.has("name") ? animObj.get("name").getAsString() : "unknown";
            double lengthSeconds = animObj.has("length") ? animObj.get("length").getAsDouble() : 1.0;
            int lengthTicks = (int) Math.ceil(lengthSeconds * 20.0);

            HitboxTrack track = new HitboxTrack(animName, lengthTicks);

            // Parse keyframes for this animation
            Map<String, BoneAnim> boneAnims = parseBoneAnimations(animObj);

            for (int tick = 0; tick <= lengthTicks; tick++) {
                double time = tick / 20.0;
                
                // Compute matrices
                Map<String, Matrix4f> globalMatrices = new HashMap<>();
                for (BbmodelParser.BoneNode rootBone : model.rootBones) {
                    computeGlobalMatrix(rootBone, new Matrix4f(), globalMatrices, boneAnims, time, model);
                }

                // Compute hitbox positions
                for (BbmodelParser.HitboxElement hb : model.hitboxElements.values()) {
                    Matrix4f boneMatrix = hb.parentBoneName != null ? globalMatrices.get(hb.parentBoneName) : new Matrix4f();
                    if (boneMatrix == null) boneMatrix = new Matrix4f();

                    Vector4f center4 = new Vector4f((float) hb.center[0], (float) hb.center[1], (float) hb.center[2], 1.0f);
                    boneMatrix.transform(center4);
                    
                    Vector3f worldOffset = new Vector3f(center4.x, center4.y, center4.z);

                    // Blockbench coordinates have X flipped in Minecraft world usually, but let's stick to the matrix
                    track.frames[tick].zones.add(new HitboxTrack.ZoneSnapshot(
                            hb.zone,
                            worldOffset,
                            hb.width,
                            hb.height,
                            hb.depth
                    ));
                }
            }
            tracks.put(animName, track);
        }

        return tracks;
    }

    private static void computeGlobalMatrix(BbmodelParser.BoneNode bone, Matrix4f parentMatrix, Map<String, Matrix4f> globalMatrices, Map<String, BoneAnim> boneAnims, double time, BbmodelParser.ParsedBbmodel model) {
        Vector3f pos = new Vector3f(0, 0, 0);
        Vector3f rot = new Vector3f(0, 0, 0);

        BoneAnim anim = boneAnims.get(bone.name);
        if (anim != null) {
            pos = anim.getInterpolatedPos(time);
            rot = anim.getInterpolatedRot(time);
        }

        float px = (float) bone.origin[0];
        float py = (float) bone.origin[1];
        float pz = (float) bone.origin[2];

        // M_local = Translate(pivot) * RotateXYZ(rotation) * Translate(-pivot) * Translate(position)
        Matrix4f localMatrix = new Matrix4f();
        localMatrix.translate(px, py, pz);
        // Z -> Y -> X order
        localMatrix.rotateZ((float) Math.toRadians(rot.z));
        localMatrix.rotateY((float) Math.toRadians(rot.y));
        localMatrix.rotateX((float) Math.toRadians(rot.x));
        
        localMatrix.translate(-px, -py, -pz);
        
        // Position from animations is in pixels, divide by 16
        localMatrix.translate(pos.x / 16f, pos.y / 16f, pos.z / 16f);

        Matrix4f globalMatrix = new Matrix4f(parentMatrix).mul(localMatrix);
        globalMatrices.put(bone.name, globalMatrix);

        for (BbmodelParser.BoneNode child : bone.children) {
            computeGlobalMatrix(child, globalMatrix, globalMatrices, boneAnims, time, model);
        }
    }

    private static Map<String, BoneAnim> parseBoneAnimations(JsonObject animObj) {
        Map<String, BoneAnim> result = new HashMap<>();
        if (!animObj.has("bones")) return result;

        JsonObject bonesObj = animObj.getAsJsonObject("bones");
        for (Map.Entry<String, JsonElement> entry : bonesObj.entrySet()) {
            String boneName = entry.getKey();
            JsonObject boneData = entry.getValue().getAsJsonObject();
            BoneAnim ba = new BoneAnim();

            if (boneData.has("position")) {
                ba.positionFrames = parseKeyframes(boneData.get("position"));
            }
            if (boneData.has("rotation")) {
                ba.rotationFrames = parseKeyframes(boneData.get("rotation"));
            }
            result.put(boneName, ba);
        }

        return result;
    }

    private static TreeMap<Double, Vector3f> parseKeyframes(JsonElement el) {
        TreeMap<Double, Vector3f> frames = new TreeMap<>();
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                try {
                    double time = Double.parseDouble(entry.getKey());
                    Vector3f val = parseVector(entry.getValue());
                    frames.put(time, val);
                } catch (NumberFormatException ignored) {}
            }
        } else if (el.isJsonArray()) {
            // single value across all time
            frames.put(0.0, parseVector(el));
        }
        return frames;
    }

    private static Vector3f parseVector(JsonElement el) {
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            return new Vector3f(
                parseNumberOrZero(arr.get(0)),
                parseNumberOrZero(arr.get(1)),
                parseNumberOrZero(arr.get(2))
            );
        } else if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("post")) {
                JsonArray arr = obj.getAsJsonArray("post");
                return new Vector3f(
                    parseNumberOrZero(arr.get(0)),
                    parseNumberOrZero(arr.get(1)),
                    parseNumberOrZero(arr.get(2))
                );
            }
        }
        return new Vector3f(0, 0, 0); // Molang or unknown
    }

    private static float parseNumberOrZero(JsonElement el) {
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            return el.getAsFloat();
        }
        return 0f;
    }

    private static class BoneAnim {
        TreeMap<Double, Vector3f> positionFrames = new TreeMap<>();
        TreeMap<Double, Vector3f> rotationFrames = new TreeMap<>();

        Vector3f getInterpolatedPos(double time) {
            return interpolate(positionFrames, time);
        }

        Vector3f getInterpolatedRot(double time) {
            return interpolate(rotationFrames, time);
        }

        private Vector3f interpolate(TreeMap<Double, Vector3f> frames, double time) {
            if (frames.isEmpty()) return new Vector3f(0, 0, 0);
            
            Map.Entry<Double, Vector3f> floor = frames.floorEntry(time);
            Map.Entry<Double, Vector3f> ceiling = frames.ceilingEntry(time);

            if (floor == null) return ceiling.getValue();
            if (ceiling == null) return floor.getValue();
            if (floor.getKey().equals(ceiling.getKey())) return floor.getValue();

            double tFloor = floor.getKey();
            double tCeil = ceiling.getKey();
            double fraction = (time - tFloor) / (tCeil - tFloor);

            Vector3f v1 = floor.getValue();
            Vector3f v2 = ceiling.getValue();
            
            return new Vector3f(v1).lerp(v2, (float) fraction);
        }
    }
}