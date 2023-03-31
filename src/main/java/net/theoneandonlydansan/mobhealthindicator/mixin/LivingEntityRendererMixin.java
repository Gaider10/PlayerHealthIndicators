package net.theoneandonlydansan.mobhealthindicator.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.theoneandonlydansan.mobhealthindicator.MobHealthIndicator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.client.render.entity.LivingEntityRenderer.getOverlay;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements FeatureRendererContext<T, M> {
    @Shadow protected abstract float getAnimationCounter(T entity, float tickDelta);

    protected LivingEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    public void renderHealth(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && MobHealthIndicator.toggled && /*!livingEntity.equals(player) &&*/ !livingEntity.isInvisibleTo(player) && player.canSee(livingEntity)) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder vertexConsumer = tessellator.getBuffer();

            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderTexture(0, DrawableHelper.GUI_ICONS_TEXTURE);
            RenderSystem.enableDepthTest();

            double d = this.dispatcher.getSquaredDistanceToCamera(livingEntity);

            int healthRed = MathHelper.ceil(livingEntity.getHealth());
            int maxHealth = MathHelper.ceil(livingEntity.getMaxHealth());
            int healthYellow = MathHelper.ceil(livingEntity.getAbsorptionAmount());

            int heartsRed = MathHelper.ceil(healthRed / 2.0F);
            boolean lastRedHalf = (healthRed & 1) == 1;
            int heartsNormal = MathHelper.ceil(maxHealth / 2.0F);
            int heartsYellow = MathHelper.ceil(healthYellow / 2.0F);
            boolean lastYellowHalf = (healthYellow & 1) == 1;
            int heartsTotal = heartsNormal + heartsYellow;

            matrixStack.push();
            Matrix4f model = matrixStack.peek().getPositionMatrix();
            vertexConsumer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

            int pixelsTotal = Math.min(heartsTotal, 10) * 8 + 1;
            float maxX = pixelsTotal / 2.0f;

            int overlay = getOverlay(livingEntity, this.getAnimationCounter(livingEntity, g));
            float height = 0;

            double heartDensity = 50F - (Math.max(4F - Math.ceil(heartsTotal/10F), -3F) * 5F);
            for (int heart = 0; heart < heartsTotal; heart++){
                if(heart % 10 == 0) {
                    tessellator.draw();
                    matrixStack.pop();

                    matrixStack.push();

                    double h = heart/heartDensity;

                    matrixStack.translate(0, livingEntity.getHeight() + 0.5f + h, 0);
                    if (this.hasLabel(livingEntity) && d <= 4096.0) {
                        matrixStack.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0D);
                        if (d < 100.0 && livingEntity instanceof PlayerEntity && livingEntity.getEntityWorld().getScoreboard().getObjectiveForSlot(2) != null) {
                            matrixStack.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0D);
                        }
                    }

                    matrixStack.multiply(this.dispatcher.getRotation());

                    float pixelSize = 0.025F;
                    matrixStack.scale(pixelSize, pixelSize, pixelSize);

                    model = matrixStack.peek().getPositionMatrix();
                    vertexConsumer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

                    height = heart;
                }

                float x = maxX - (heart - height) * 8;

                drawHeart(model, vertexConsumer, x, 0, overlay, light);

                int type;
                if(heart < heartsRed) {
                    type = 2 * 2;
                    if(heart == heartsRed - 1 && lastRedHalf) type += 1;
                } else if(heart < heartsNormal) {
                    type = 0;
                } else {
                    type = 8 * 2;
                    if(heart == heartsTotal - 1 && lastYellowHalf) type += 1;
                }

                if(type != 0) drawHeart(model, vertexConsumer, x, type, overlay, light);
            }

            tessellator.draw();
            matrixStack.pop();
        }
    }

    private static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, int type, int overlay, int light) {
        float minU = 16F / 256F + type * 9F / 256F;
        float maxU = minU + 9F / 256F;
        float minV = 0;
        float maxV = minV + 9F / 256F;

        float heartSize = 9F;

        drawVertex(model, vertexConsumer, x, 0F - heartSize, minU, maxV, overlay, light);
        drawVertex(model, vertexConsumer, x - heartSize, 0F - heartSize, maxU, maxV, overlay, light);
        drawVertex(model, vertexConsumer, x - heartSize, 0F, maxU, minV, overlay, light);
        drawVertex(model, vertexConsumer, x, 0F, minU, minV, overlay, light);
    }

    private static void drawVertex(Matrix4f model, VertexConsumer vertices, float x, float y, float u, float v, int overlay, int light) {
        vertices.vertex(model, x, y, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).texture(u, v).overlay(overlay).light(light).normal(0F, 0F, 0F).next();
    }
}
