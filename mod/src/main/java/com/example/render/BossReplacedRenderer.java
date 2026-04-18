package com.example.render;

import com.example.anim.BossAnimController;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.nbt.NbtCompound;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BossReplacedRenderer extends GeoReplacedEntityRenderer<ArmorStandEntity, BossReplacedRenderer.DummyAnimatable> {
    private final ArmorStandEntityRenderer vanillaRenderer;

    public BossReplacedRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new BossGeoModel(), null);
        this.vanillaRenderer = new ArmorStandEntityRenderer(renderManager);
    }

    @Override
    public void render(ArmorStandEntity entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        if (isBoss(entity)) {
            this.animatable = new DummyAnimatable(entity.getId());
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } else {
            this.vanillaRenderer.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }
    }

    private boolean isBoss(ArmorStandEntity entity) {
        // ... (rest of the method remains same)
    }

    public static class DummyAnimatable implements GeoReplacedEntity {
        public final int entityId;
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        public DummyAnimatable(int entityId) {
            this.entityId = entityId;
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this, "boss_controller", 3, state -> {
                BossAnimController.AnimState current = BossAnimController.states.get(this.entityId);
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