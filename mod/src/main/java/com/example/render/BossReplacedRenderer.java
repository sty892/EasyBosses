package com.example.render;

import com.example.anim.BossAnimController;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.decoration.ArmorStandEntity;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BossReplacedRenderer extends GeoReplacedEntityRenderer<ArmorStandEntity, BossReplacedRenderer.DummyAnimatable> {

    public BossReplacedRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new BossGeoModel(), new DummyAnimatable());
    }

    public static class DummyAnimatable implements GeoReplacedEntity {
        public int entityId;
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this, "boss_controller", 3, state -> {
                BossAnimController.AnimState current = BossAnimController.states.get(entityId);
                if (current == null) return software.bernie.geckolib.animation.PlayState.STOP;
                
                software.bernie.geckolib.animation.RawAnimation anim = software.bernie.geckolib.animation.RawAnimation.begin().thenLoop(current.animName());
                return state.setAndContinue(anim);
            }));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public net.minecraft.entity.EntityType<?> getReplacingEntityType() {
            return net.minecraft.entity.EntityType.ARMOR_STAND;
        }
    }
}