package com.example.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

public class GeckoExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String exportGeo(BbmodelParser.ParsedBbmodel model, String id) {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.12.0");
        
        JsonArray geometryArr = new JsonArray();
        JsonObject geometry = new JsonObject();
        
        JsonObject description = new JsonObject();
        description.addProperty("identifier", "geometry." + id);
        
        int texWidth = 64, texHeight = 64;
        if (model.rawRoot.has("resolution")) {
            JsonObject res = model.rawRoot.getAsJsonObject("resolution");
            texWidth = res.has("width") ? res.get("width").getAsInt() : 64;
            texHeight = res.has("height") ? res.get("height").getAsInt() : 64;
        }
        description.addProperty("texture_width", texWidth);
        description.addProperty("texture_height", texHeight);
        
        geometry.add("description", description);
        
        JsonArray bonesArr = new JsonArray();
        for (BbmodelParser.BoneNode rootBone : model.rootBones) {
            exportBone(rootBone, null, bonesArr, model);
        }
        geometry.add("bones", bonesArr);
        
        geometryArr.add(geometry);
        root.add("minecraft:geometry", geometryArr);
        
        return GSON.toJson(root);
    }

    private static void exportBone(BbmodelParser.BoneNode bone, String parentName, JsonArray bonesArr, BbmodelParser.ParsedBbmodel model) {
        if (bone.name.startsWith("hitbox:")) return;

        JsonObject boneObj = new JsonObject();
        boneObj.addProperty("name", bone.name);
        if (parentName != null) {
            boneObj.addProperty("parent", parentName);
        }
        
        JsonArray pivot = new JsonArray();
        pivot.add(-bone.origin[0] * 16.0); // usually X is flipped in bedrock
        pivot.add(bone.origin[1] * 16.0);
        pivot.add(bone.origin[2] * 16.0);
        boneObj.add("pivot", pivot);

        // Find elements for this bone
        JsonArray cubes = new JsonArray();
        if (model.rawRoot.has("elements")) {
            for (JsonElement el : model.rawRoot.getAsJsonArray("elements")) {
                JsonObject element = el.getAsJsonObject();
                if (element.has("uuid") && bone.childElementUUIDs.contains(element.get("uuid").getAsString())) {
                    if (element.has("name") && element.get("name").getAsString().startsWith("hitbox:")) {
                        continue;
                    }
                    JsonObject cube = new JsonObject();
                    
                    if (element.has("origin")) {
                        JsonArray originArr = element.getAsJsonArray("origin");
                        JsonArray cubeOrigin = new JsonArray();
                        cubeOrigin.add(-originArr.get(0).getAsDouble());
                        cubeOrigin.add(originArr.get(1).getAsDouble());
                        cubeOrigin.add(originArr.get(2).getAsDouble());
                        cube.add("origin", cubeOrigin);
                    }
                    
                    if (element.has("from") && element.has("to")) {
                        JsonArray fromArr = element.getAsJsonArray("from");
                        JsonArray toArr = element.getAsJsonArray("to");
                        JsonArray size = new JsonArray();
                        size.add(toArr.get(0).getAsDouble() - fromArr.get(0).getAsDouble());
                        size.add(toArr.get(1).getAsDouble() - fromArr.get(1).getAsDouble());
                        size.add(toArr.get(2).getAsDouble() - fromArr.get(2).getAsDouble());
                        cube.add("size", size);
                        
                        // Origin in bedrock format is usually the 'from' point but flipped X, or the center.
                        // We will just put the minimum 'from' coordinates.
                        JsonArray cubeOrigin = new JsonArray();
                        cubeOrigin.add(-toArr.get(0).getAsDouble()); // flip X and use 'to' because of flipping
                        cubeOrigin.add(fromArr.get(1).getAsDouble());
                        cubeOrigin.add(fromArr.get(2).getAsDouble());
                        cube.add("origin", cubeOrigin);
                    }

                    if (element.has("faces")) {
                        JsonObject faces = element.getAsJsonObject("faces");
                        if (faces.has("north")) {
                            JsonObject north = faces.getAsJsonObject("north");
                            if (north.has("uv")) {
                                JsonArray uvArr = north.getAsJsonArray("uv");
                                JsonArray uv = new JsonArray();
                                uv.add(uvArr.get(0).getAsDouble());
                                uv.add(uvArr.get(1).getAsDouble());
                                cube.add("uv", uv);
                            }
                        }
                    }

                    cubes.add(cube);
                }
            }
        }
        if (cubes.size() > 0) {
            boneObj.add("cubes", cubes);
        }

        bonesArr.add(boneObj);

        for (BbmodelParser.BoneNode child : bone.children) {
            exportBone(child, bone.name, bonesArr, model);
        }
    }

    public static String exportAnimations(BbmodelParser.ParsedBbmodel model) {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.8.0");
        
        JsonObject animations = new JsonObject();
        
        for (JsonElement animEl : model.animationsJson) {
            JsonObject animObj = animEl.getAsJsonObject();
            String name = animObj.has("name") ? animObj.get("name").getAsString() : "unknown";
            
            JsonObject outAnim = new JsonObject();
            if (animObj.has("loop")) {
                String loopStr = animObj.get("loop").getAsString();
                outAnim.addProperty("loop", loopStr.equals("loop"));
            }
            if (animObj.has("length")) {
                outAnim.addProperty("animation_length", animObj.get("length").getAsDouble());
            }
            
            if (animObj.has("bones")) {
                JsonObject bones = animObj.getAsJsonObject("bones");
                JsonObject outBones = new JsonObject();
                
                for (Map.Entry<String, JsonElement> entry : bones.entrySet()) {
                    String boneName = entry.getKey();
                    JsonObject boneData = entry.getValue().getAsJsonObject();
                    JsonObject outBoneData = new JsonObject();
                    
                    if (boneData.has("rotation")) {
                        outBoneData.add("rotation", convertKeyframes(boneData.get("rotation"), 1.0));
                    }
                    if (boneData.has("position")) {
                        // "position делить на 16"
                        outBoneData.add("position", convertKeyframes(boneData.get("position"), 1.0 / 16.0));
                    }
                    outBones.add(boneName, outBoneData);
                }
                outAnim.add("bones", outBones);
            }
            
            animations.add(name, outAnim);
        }
        
        root.add("animations", animations);
        
        return GSON.toJson(root);
    }

    private static JsonElement convertKeyframes(JsonElement el, double multiplier) {
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            JsonObject outObj = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                outObj.add(entry.getKey(), convertVector(entry.getValue(), multiplier));
            }
            return outObj;
        } else if (el.isJsonArray()) {
            return convertVector(el, multiplier);
        }
        return el;
    }

    private static JsonArray convertVector(JsonElement el, double multiplier) {
        JsonArray outArr = new JsonArray();
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            outArr.add(parseNumberOrZero(arr.get(0)) * multiplier);
            outArr.add(parseNumberOrZero(arr.get(1)) * multiplier);
            outArr.add(parseNumberOrZero(arr.get(2)) * multiplier);
        } else if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("post")) {
                JsonArray arr = obj.getAsJsonArray("post");
                outArr.add(parseNumberOrZero(arr.get(0)) * multiplier);
                outArr.add(parseNumberOrZero(arr.get(1)) * multiplier);
                outArr.add(parseNumberOrZero(arr.get(2)) * multiplier);
            } else {
                outArr.add(0); outArr.add(0); outArr.add(0);
            }
        } else {
            outArr.add(0); outArr.add(0); outArr.add(0);
        }
        return outArr;
    }

    private static float parseNumberOrZero(JsonElement el) {
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            return el.getAsFloat();
        }
        return 0f;
    }
}