package com.example.render;

import com.example.anim.BossAnimController;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class BossGeoModel extends GeoModel<BossReplacedRenderer.DummyAnimatable> {
    
    @Override
    public Identifier getModelResource(BossReplacedRenderer.DummyAnimatable entity) {
        String bossId = BossAnimController.bossIds.get(entity.entityId);
        if (bossId == null) return Identifier.of("bossframework", "geo/unknown.geo.json");
        return Identifier.of("bossframework", "geo/" + bossId + ".geo.json");
    }
    
    @Override
    public Identifier getTextureResource(BossReplacedRenderer.DummyAnimatable entity) {
        String bossId = BossAnimController.bossIds.get(entity.entityId);
        if (bossId == null) return Identifier.of("bossframework", "textures/entity/unknown.png");
        return Identifier.of("bossframework", "textures/entity/" + bossId + ".png");
    }
    
    @Override
    public Identifier getAnimationResource(BossReplacedRenderer.DummyAnimatable entity) {
        String bossId = BossAnimController.bossIds.get(entity.entityId);
        if (bossId == null) return Identifier.of("bossframework", "animations/unknown.animations.json");
        return Identifier.of("bossframework", "animations/" + bossId + ".animations.json");
    }
}