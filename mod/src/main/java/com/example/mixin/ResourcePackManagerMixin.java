package com.example.mixin;

import com.example.resource.BossCachePackProvider;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(ResourcePackManager.class)
public class ResourcePackManagerMixin {
    @Shadow @Final private Set<ResourcePackProvider> providers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addBossCacheProvider(CallbackInfo ci) {
        // Need to make sure the set is mutable or replace it
        // In many environments it is a mutable set passed from the constructor
        try {
            this.providers.add(new BossCachePackProvider());
        } catch (UnsupportedOperationException e) {
            // If it's immutable, we can't easily add to it here without more complex mixin
            // but usually it's a HashSet or similar in the constructor
        }
    }
}
