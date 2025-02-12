package ladysnake.effective.mixin.spectral_arrows;

import com.sammy.lodestone.setup.LodestoneRenderLayers;
import com.sammy.lodestone.systems.rendering.PositionTrackedEntity;
import com.sammy.lodestone.systems.rendering.VFXBuilders;
import com.sammy.lodestone.systems.rendering.particle.ParticleBuilders;
import ladysnake.effective.Effective;
import ladysnake.effective.EffectiveConfig;
import ladysnake.effective.particle.contracts.ColoredParticleInitialData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.ArrayList;

import static com.sammy.lodestone.handlers.RenderHandler.DELAYED_RENDER;

@Mixin(ProjectileEntityRenderer.class)
public abstract class SpectralArrowTrailRenderer<T extends PersistentProjectileEntity> extends EntityRenderer<T> {
	private static final Identifier LIGHT_TRAIL = new Identifier(Effective.MODID, "textures/vfx/light_trail.png");
	private static final RenderLayer LIGHT_TYPE = LodestoneRenderLayers.ADDITIVE_TEXTURE.apply(LIGHT_TRAIL);

	protected SpectralArrowTrailRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	// spectral arrow trail and twinkle
	@Inject(method = "render(Lnet/minecraft/entity/projectile/PersistentProjectileEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
	public void render(T entity, float entityYaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
		// new render
		if (EffectiveConfig.improvedSpectralArrows && entity instanceof SpectralArrowEntity spectralArrowEntity && !spectralArrowEntity.isInvisible()) {
			ColoredParticleInitialData data = new ColoredParticleInitialData(0xFFFF77);

			// trail
			matrixStack.push();
			ArrayList<Vec3d> positions = new ArrayList<>(((PositionTrackedEntity) spectralArrowEntity).getPastPositions());
			VFXBuilders.WorldVFXBuilder builder = VFXBuilders.createWorld().setPosColorTexLightmapDefaultFormat();

			float size = 0.15f;
			float alpha = 1f;

			float x = (float) MathHelper.lerp(tickDelta, spectralArrowEntity.prevX, spectralArrowEntity.getX());
			float y = (float) MathHelper.lerp(tickDelta, spectralArrowEntity.prevY, spectralArrowEntity.getY());
			float z = (float) MathHelper.lerp(tickDelta, spectralArrowEntity.prevZ, spectralArrowEntity.getZ());

			builder.setColor(new Color(data.color)).setOffset(-x, -y, -z)
				.setAlpha(alpha)
				.renderTrail(
					DELAYED_RENDER.getBuffer(LIGHT_TYPE),
					matrixStack,
					positions.stream()
						.map(p -> new Vector4f((float) p.x, (float) p.y, (float) p.z, 1))
						.toList(),
					f -> MathHelper.sqrt(f) * size,
					f -> builder.setAlpha((float) Math.cbrt(Math.max(0, (alpha * f) - 0.1f)))
				)
				.renderTrail(
					DELAYED_RENDER.getBuffer(LIGHT_TYPE),
					matrixStack,
					positions.stream()
						.map(p -> new Vector4f((float) p.x, (float) p.y, (float) p.z, 1))
						.toList(),
					f -> (MathHelper.sqrt(f) * size) / 1.5f,
					f -> builder.setAlpha((float) Math.cbrt(Math.max(0, (((alpha * f) / 1.5f) - 0.1f))))
				);

			matrixStack.pop();

			// twinkles
			if ((spectralArrowEntity.world.getRandom().nextInt(100) + 1) <= 5 && !MinecraftClient.getInstance().isPaused()) {
				float spreadDivider = 4f;
				ParticleBuilders.create(Effective.ALLAY_TWINKLE)
					.setColor(new Color(data.color), new Color(data.color))
					.setAlpha(0.9f)
					.setScale(0.06f)
					.setLifetime(15)
					.setMotion(0, 0.05f, 0)
					.spawn(spectralArrowEntity.world, spectralArrowEntity.getClientCameraPosVec(MinecraftClient.getInstance().getTickDelta()).x + spectralArrowEntity.world.getRandom().nextGaussian() / spreadDivider, spectralArrowEntity.getClientCameraPosVec(MinecraftClient.getInstance().getTickDelta()).y - 0.2f + spectralArrowEntity.world.getRandom().nextGaussian() / spreadDivider, spectralArrowEntity.getClientCameraPosVec(MinecraftClient.getInstance().getTickDelta()).z + spectralArrowEntity.world.getRandom().nextGaussian() / spreadDivider);
			}
		}
	}

}
