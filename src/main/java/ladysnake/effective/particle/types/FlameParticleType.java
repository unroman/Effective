package ladysnake.effective.particle.types;

import com.mojang.serialization.Codec;
import com.sammy.lodestone.systems.rendering.particle.world.WorldParticleEffect;
import ladysnake.effective.particle.FlameParticle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.impl.client.particle.FabricSpriteProviderImpl;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleType;

@Environment(EnvType.CLIENT)
public class FlameParticleType extends ParticleType<WorldParticleEffect> {

	public FlameParticleType() {
		super(false, WorldParticleEffect.DESERIALIZER);
	}

	@Override
	public boolean shouldAlwaysSpawn() {
		return true;
	}

	@Override
	public Codec<WorldParticleEffect> getCodec() {
		return WorldParticleEffect.codecFor(this);
	}

	public record Factory(SpriteProvider sprite) implements ParticleFactory<WorldParticleEffect> {
		@Override
		public Particle createParticle(WorldParticleEffect data, ClientWorld world, double x, double y, double z, double mx, double my, double mz) {
			return new FlameParticle(world, data, (FabricSpriteProviderImpl) sprite, x, y, z, mx, my, mz);
		}
	}
}
