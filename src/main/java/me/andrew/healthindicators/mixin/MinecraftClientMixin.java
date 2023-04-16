package me.andrew.healthindicators.mixin;

import me.andrew.healthindicators.HealthIndicatorsMod;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void inject_tick(CallbackInfo ci) {
        HealthIndicatorsMod.onTick((MinecraftClient)(Object) this);
    }
}
