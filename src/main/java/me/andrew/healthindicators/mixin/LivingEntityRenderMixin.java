package me.andrew.healthindicators.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.andrew.healthindicators.Config;
import me.andrew.healthindicators.HealthIndicatorsMod;
import me.andrew.healthindicators.HeartType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.texture.GuiAtlasManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRenderMixin<T extends LivingEntity, S extends LivingEntityRenderState> {

    @Shadow protected abstract boolean hasLabel(T livingEntity, double d);

    private static final MinecraftClient minecraftClient = MinecraftClient.getInstance();
    
    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL")
    )
    private void renderHearts(S livingEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (
                livingEntityRenderState instanceof PlayerEntityRenderState playerEntityRenderState
                && (Object) this instanceof PlayerEntityRenderer playerEntityRenderer
        ) {
            if (!Config.getRenderingEnabled()) return;
            if (playerEntityRenderState.invisibleToPlayer) return;
            if (minecraftClient.world == null) return;
            
            AbstractClientPlayerEntity player = null;
            
            for (AbstractClientPlayerEntity playerEntity : minecraftClient.world.getPlayers()) {
                if (playerEntity.getName().getLiteralString().equals(playerEntityRenderState.name)) {
                    player = playerEntity;
                    break;
                }
            }

            if (player == null) return;

            matrixStack.push();

            matrixStack.translate(0, playerEntityRenderState.height + 0.5f, 0);
            if (this.hasLabel((T) player, playerEntityRenderState.squaredDistanceToCamera) && playerEntityRenderState.squaredDistanceToCamera <= 4096.0) {
                matrixStack.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0D);
                if (playerEntityRenderState.squaredDistanceToCamera < 100.0 && player.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
                    matrixStack.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0D);
                }
            }

            matrixStack.multiply(((EntityRendererAccessor) playerEntityRenderer).getDispatcher().getRotation());
            matrixStack.scale(-1, 1, 1);

            float pixelSize = 0.025F;
            matrixStack.scale(pixelSize, pixelSize, pixelSize);
            matrixStack.translate(0, Config.getHeartOffset(), 0);

            GuiAtlasManager guiAtlasManager = MinecraftClient.getInstance().getGuiAtlasManager();

            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
            RenderSystem.setShaderTexture(0, guiAtlasManager.getSprite(HeartType.EMPTY.texture).getAtlasId());
            RenderSystem.enableDepthTest();
            BufferBuilder vertexConsumer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

            Matrix4f model = matrixStack.peek().getPositionMatrix();

            int healthRed = MathHelper.ceil(player.getHealth());
            int maxHealth = MathHelper.ceil(player.getMaxHealth());
            int healthYellow = MathHelper.ceil(player.getAbsorptionAmount());

            int heartsRed = MathHelper.ceil(healthRed / 2.0f);
            boolean lastRedHalf = (healthRed & 1) == 1;
            int heartsNormal = MathHelper.ceil(maxHealth / 2.0f);
            int heartsYellow = MathHelper.ceil(healthYellow / 2.0f);
            boolean lastYellowHalf = (healthYellow & 1) == 1;
            int heartsTotal = heartsNormal + heartsYellow;

            int heartsPerRow = Config.getHeartStackingEnabled() ? 10 : heartsTotal;
            int rowsTotal = (heartsTotal + heartsPerRow - 1) / heartsPerRow;
            int rowOffset = Math.max(10 - (rowsTotal - 2), 3);

            int pixelsTotal = Math.min(heartsTotal, heartsPerRow) * 8 + 1;
            float maxX = pixelsTotal / 2.0f;
            for (int heart = 0; heart < heartsTotal; heart++){
                int row = heart / heartsPerRow;
                int col = heart % heartsPerRow;

                float x = maxX - col * 8;
                float y = row * rowOffset;
                float z = row * 0.01F;
                drawHeart(model, vertexConsumer, x, y, z, HeartType.EMPTY, guiAtlasManager);

                HeartType type;
                if (heart < heartsRed) {
                    type = HeartType.RED_FULL;
                    if (heart == heartsRed - 1 && lastRedHalf) {
                        type = HeartType.RED_HALF;
                    }
                } else if (heart < heartsNormal) {
                    type = HeartType.EMPTY;
                } else {
                    type = HeartType.YELLOW_FULL;
                    if (heart == heartsTotal - 1 && lastYellowHalf) {
                        type = HeartType.YELLOW_HALF;
                    }
                }
                if (type != HeartType.EMPTY) {
                    drawHeart(model, vertexConsumer, x, y, z, type, guiAtlasManager);
                }
            }

            BufferRenderer.drawWithGlobalProgram(vertexConsumer.end());

            matrixStack.pop();
        }
    }

    @Unique
    private static boolean shouldRenderHeartsForEntity(Entity entity) {
        if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity) {
            return !abstractClientPlayerEntity.isMainPlayer() && !abstractClientPlayerEntity.isInvisibleTo(MinecraftClient.getInstance().player);
        }

        return false;
    }

    @Unique
    private static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, float y, float z, HeartType type, GuiAtlasManager guiAtlasManager){
        Sprite sprite = guiAtlasManager.getSprite(type.texture);

        float minU = sprite.getMinU();
        float maxU = sprite.getMaxU();
        float minV = sprite.getMinV();
        float maxV = sprite.getMaxV();

        float heartSize = 9F;

        drawVertex(model, vertexConsumer, x, y - heartSize, z, minU, maxV);
        drawVertex(model, vertexConsumer, x - heartSize, y - heartSize, z, maxU, maxV);
        drawVertex(model, vertexConsumer, x - heartSize, y, z, maxU, minV);
        drawVertex(model, vertexConsumer, x, y, z, minU, minV);
    }

    @Unique
    private static void drawVertex(Matrix4f model, VertexConsumer vertices, float x, float y, float z, float u, float v) {
        vertices.vertex(model, x, y, z).texture(u, v);
    }
}
