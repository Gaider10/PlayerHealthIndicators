package me.andrew.healthindicators.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.andrew.healthindicators.Config;
import me.andrew.healthindicators.HeartType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final Identifier ICONS = new Identifier("textures/gui/icons.png");

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN")
    )
    public void renderHealth(AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        if (!Config.getRenderingEnabled()) return;

        if (!shouldRenderHeartsForEntity(abstractClientPlayerEntity)) return;

        matrixStack.push();

        double d = this.dispatcher.getSquaredDistanceToCamera(abstractClientPlayerEntity);

        matrixStack.translate(0, abstractClientPlayerEntity.getHeight() + 0.5f, 0);
        if (this.hasLabel(abstractClientPlayerEntity) && d <= 4096.0) {
            matrixStack.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0D);
            if (d < 100.0 && abstractClientPlayerEntity.getScoreboard().getObjectiveForSlot(2) != null) {
                matrixStack.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0D);
            }
        }

        matrixStack.multiply(this.dispatcher.getRotation());
//            matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(mc.gameRenderer.getCamera().getPitch()));

        float pixelSize = 0.025F;
        matrixStack.scale(pixelSize, pixelSize, pixelSize);
        matrixStack.translate(0, Config.getHeartOffset(), 0);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexConsumer = tessellator.getBuffer();

        vertexConsumer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, ICONS);
        RenderSystem.enableDepthTest();

        Matrix4f model = matrixStack.peek().getPositionMatrix();

        int healthRed = MathHelper.ceil(abstractClientPlayerEntity.getHealth());
        int maxHealth = MathHelper.ceil(abstractClientPlayerEntity.getMaxHealth());
        int healthYellow = MathHelper.ceil(abstractClientPlayerEntity.getAbsorptionAmount());

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
            drawHeart(model, vertexConsumer, x, y, z, HeartType.EMPTY);

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
                drawHeart(model, vertexConsumer, x, y, z, type);
            }
        }

        tessellator.draw();

        matrixStack.pop();
    }

    private static boolean shouldRenderHeartsForEntity(Entity entity) {
        if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity) {
            return !abstractClientPlayerEntity.isMainPlayer() && !abstractClientPlayerEntity.isInvisibleTo(MinecraftClient.getInstance().player);
        }

        return false;
    }

    private static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, float y, float z, HeartType type){
        float minU = type.u / 256F;
        float maxU = minU + 9F / 256F;
        float minV = type.v / 256F;
        float maxV = minV + 9F / 256F;

        float heartSize = 9F;

        drawVertex(model, vertexConsumer, x, y - heartSize, z, minU, maxV);
        drawVertex(model, vertexConsumer, x - heartSize, y - heartSize, z, maxU, maxV);
        drawVertex(model, vertexConsumer, x - heartSize, y, z, maxU, minV);
        drawVertex(model, vertexConsumer, x, y, z, minU, minV);
    }

    private static void drawVertex(Matrix4f model, VertexConsumer vertices, float x, float y, float z, float u, float v) {
        vertices.vertex(model, x, y, z).texture(u, v).next();
    }
}
