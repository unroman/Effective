package ladysnake.effective.mixin.spectral_arrows;

import ladysnake.effective.EffectiveConfig;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class SpectralArrowEmissiveEnforcer<T extends Entity> {
	@Inject(method = "getBlockLight", at = @At("RETURN"), cancellable = true)
	protected void getBlockLight(T entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (EffectiveConfig.improvedSpectralArrows && entity instanceof SpectralArrowEntity spectralArrowEntity) {
			cir.setReturnValue(15);
		}
	}
}
