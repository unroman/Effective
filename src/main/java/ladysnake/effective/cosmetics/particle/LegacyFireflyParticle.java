package ladysnake.effective.cosmetics.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import ladysnake.effective.cosmetics.EffectiveCosmetics;
import ladysnake.effective.cosmetics.particle.type.LegacyFireflyParticleType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.*;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class LegacyFireflyParticle extends SpriteBillboardParticle {
	protected static final float BLINK_STEP = 0.05f;
	private final SpriteProvider spriteProvider;
	private final boolean isAttractedByLight = true;
	protected float nextAlphaGoal = 0f;
	protected double xTarget;
	protected double yTarget;
	protected double zTarget;
	protected int targetChangeCooldown = 0;
	protected int maxHeight;
	private BlockPos lightTarget;

	public LegacyFireflyParticle(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider) {
		super(world, x, y, z, 0f, 0f, 0f);
		this.spriteProvider = spriteProvider;

		this.scale *= 0.25f + random.nextFloat() * 0.50f;
		this.maxAge = ThreadLocalRandom.current().nextInt(400, 1201); // live between 20 seconds and one minute
		this.maxHeight = 4;
		this.collidesWithWorld = true;
		this.setSpriteForAge(spriteProvider);
		this.colorAlpha = 0f;
		this.collidesWithWorld = false;
	}

	public static boolean canFlyThroughBlock(World world, BlockPos blockPos, BlockState blockState) {
		return !blockState.shouldSuffocate(world, blockPos);
	}

	public ParticleTextureSheet getType() {
		return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
		Vec3d vec3d = camera.getPos();
		float f = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - vec3d.getX());
		float g = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - vec3d.getY());
		float h = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - vec3d.getZ());
		Quaternion quaternion2;
		if (this.angle == 0.0F) {
			quaternion2 = camera.getRotation();
		} else {
			quaternion2 = new Quaternion(camera.getRotation());
			float i = MathHelper.lerp(tickDelta, this.prevAngle, this.angle);
			quaternion2.hamiltonProduct(Vec3f.POSITIVE_Z.getRadialQuaternion(i));
		}

		Vec3f Vec3f = new Vec3f(-1.0F, -1.0F, 0.0F);
		Vec3f.rotate(quaternion2);
		Vec3f[] Vec3fs = new Vec3f[]{new Vec3f(-1.0F, -1.0F, 0.0F), new Vec3f(-1.0F, 1.0F, 0.0F), new Vec3f(1.0F, 1.0F, 0.0F), new Vec3f(1.0F, -1.0F, 0.0F)};
		float j = this.getSize(tickDelta);

		for (int k = 0; k < 4; ++k) {
			Vec3f Vec3f2 = Vec3fs[k];
			Vec3f2.rotate(quaternion2);
			Vec3f2.scale(j);
			Vec3f2.add(f, g, h);
		}

		float minU = this.getMinU();
		float maxU = this.getMaxU();
		float minV = this.getMinV();
		float maxV = this.getMaxV();
		int l = 15728880;
		float a = Math.min(1f, Math.max(0f, this.colorAlpha));

		// colored layer
		vertexConsumer.vertex(Vec3fs[0].getX(), Vec3fs[0].getY(), Vec3fs[0].getZ()).uv(maxU, minV + (maxV - minV) / 2.0F).color(this.colorRed, this.colorGreen, this.colorBlue, a).light(l).next();
		vertexConsumer.vertex(Vec3fs[1].getX(), Vec3fs[1].getY(), Vec3fs[1].getZ()).uv(maxU, minV).color(this.colorRed, this.colorGreen, this.colorBlue, a).light(l).next();
		vertexConsumer.vertex(Vec3fs[2].getX(), Vec3fs[2].getY(), Vec3fs[2].getZ()).uv(minU, minV).color(this.colorRed, this.colorGreen, this.colorBlue, a).light(l).next();
		vertexConsumer.vertex(Vec3fs[3].getX(), Vec3fs[3].getY(), Vec3fs[3].getZ()).uv(minU, minV + (maxV - minV) / 2.0F).color(this.colorRed, this.colorGreen, this.colorBlue, a).light(l).next();

		// white center
		vertexConsumer.vertex(Vec3fs[0].getX(), Vec3fs[0].getY(), Vec3fs[0].getZ()).uv(maxU, maxV).color(1f, 1f, 1f, 0.5f).light(l).next();
		vertexConsumer.vertex(Vec3fs[1].getX(), Vec3fs[1].getY(), Vec3fs[1].getZ()).uv(maxU, minV + (maxV - minV) / 2.0F).color(1f, 1f, 1f, 0.5f).light(l).next();
		vertexConsumer.vertex(Vec3fs[2].getX(), Vec3fs[2].getY(), Vec3fs[2].getZ()).uv(minU, minV + (maxV - minV) / 2.0F).color(1f, 1f, 1f, 0.5f).light(l).next();
		vertexConsumer.vertex(Vec3fs[3].getX(), Vec3fs[3].getY(), Vec3fs[3].getZ()).uv(minU, maxV).color(1f, 1f, 1f, 0.5f).light(l).next();
	}

	public void tick() {
		this.prevPosX = this.x;
		this.prevPosY = this.y;
		this.prevPosZ = this.z;

		// fade and die on daytime or if old enough unless fireflies can spawn any time of day
		if ((!world.getDimension().hasFixedTime() && !EffectiveCosmetics.isNightTime(world)) || this.age++ >= this.maxAge) {
			nextAlphaGoal = 0;
			if (this.colorAlpha <= 0.01f) {
				this.markDead();
			}
		}

		// blinking
		if (this.colorAlpha > nextAlphaGoal - BLINK_STEP && this.colorAlpha < nextAlphaGoal + BLINK_STEP) {
			nextAlphaGoal = random.nextFloat();
		} else {
			if (nextAlphaGoal > this.colorAlpha) {
				this.colorAlpha = Math.min(this.colorAlpha + BLINK_STEP, 1f);
			} else if (nextAlphaGoal < this.colorAlpha) {
				this.colorAlpha = Math.max(this.colorAlpha - BLINK_STEP, 0f);
			}
		}

		this.targetChangeCooldown -= (new Vec3d(x, y, z).squaredDistanceTo(prevPosX, prevPosY, prevPosZ) < 0.0125) ? 10 : 1;

		if ((this.world.getTime() % 20 == 0) && ((xTarget == 0 && yTarget == 0 && zTarget == 0) || new Vec3d(x, y, z).squaredDistanceTo(xTarget, yTarget, zTarget) < 9 || targetChangeCooldown <= 0)) {
			selectBlockTarget();
		}

		Vec3d targetVector = new Vec3d(this.xTarget - this.x, this.yTarget - this.y, this.zTarget - this.z);
		double length = targetVector.length();
		targetVector = targetVector.multiply(0.1 / length);

		BlockPos blockPos = new BlockPos(this.x, this.y - 0.1, this.z);
		if (!canFlyThroughBlock(this.world, blockPos, this.world.getBlockState(blockPos))) {
			velocityX = (0.9) * velocityX + (0.1) * targetVector.x;
			velocityY = 0.05;
			velocityZ = (0.9) * velocityZ + (0.1) * targetVector.z;
		} else {
			velocityX = (0.9) * velocityX + (0.1) * targetVector.x;
			velocityY = (0.9) * velocityY + (0.1) * targetVector.y;
			velocityZ = (0.9) * velocityZ + (0.1) * targetVector.z;
		}
		if (!new BlockPos(x, y, z).equals(this.getTargetPosition())) {
			this.move(velocityX, velocityY, velocityZ);
		}
	}

	private void selectBlockTarget() {
		if (this.lightTarget == null) {
			// Behaviour
			double groundLevel = 0;
			for (int i = 0; i < 20; i++) {
				BlockPos checkedPos = new BlockPos(this.x, this.y - i, this.z);
				BlockState checkedBlock = this.world.getBlockState(checkedPos);
				if (canFlyThroughBlock(this.world, checkedPos, checkedBlock)) {
					groundLevel = this.y - i;
				}
				if (groundLevel != 0) break;
			}

			this.xTarget = this.x + random.nextGaussian() * 10;
			this.yTarget = Math.min(Math.max(this.y + random.nextGaussian() * 2, groundLevel), groundLevel + maxHeight);
			this.zTarget = this.z + random.nextGaussian() * 10;

			BlockPos targetPos = new BlockPos(this.xTarget, this.yTarget, this.zTarget);
			if (!canFlyThroughBlock(this.world, targetPos, this.world.getBlockState(targetPos))) {
				this.yTarget += 1;
			}

			if (this.isAttractedByLight) {
				this.lightTarget = getMostLitBlockAround();
			}
		} else {
			this.xTarget = this.lightTarget.getX() + random.nextGaussian();
			this.yTarget = this.lightTarget.getY() + random.nextGaussian();
			this.zTarget = this.lightTarget.getZ() + random.nextGaussian();

			this.x = this.lightTarget.getX();
			this.y = this.lightTarget.getY() + 1;
			this.z = this.lightTarget.getZ();

			if (this.world.getLightLevel(LightType.BLOCK, new BlockPos(x, y, z)) > 0 && !this.world.isDay()) {
				this.lightTarget = getMostLitBlockAround();
			} else {
				this.lightTarget = null;
			}
		}

		targetChangeCooldown = random.nextInt() % 100;
	}

	public BlockPos getTargetPosition() {
		return new BlockPos(this.xTarget, this.yTarget + 0.5, this.zTarget);
	}

	private BlockPos getMostLitBlockAround() {
		HashMap<BlockPos, Integer> randBlocks = new HashMap<>();

		// get blocks adjacent to the fly
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					BlockPos bp = new BlockPos(this.x + x, this.y + y, this.z + z);
					randBlocks.put(bp, this.world.getLightLevel(LightType.BLOCK, bp));
				}
			}
		}

		// get other random blocks to find a different light source
		for (int i = 0; i < 15; i++) {
			BlockPos randBP = new BlockPos(this.x + random.nextGaussian() * 10, this.y + random.nextGaussian() * 10, this.z + random.nextGaussian() * 10);
			randBlocks.put(randBP, this.world.getLightLevel(LightType.BLOCK, randBP));
		}

		return randBlocks.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
	}

	@Environment(EnvType.CLIENT)
	public static class DefaultFactory implements ParticleFactory<DefaultParticleType> {
		private final SpriteProvider spriteProvider;

		public DefaultFactory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Nullable
		@Override
		public Particle createParticle(DefaultParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
			LegacyFireflyParticle instance = new LegacyFireflyParticle(world, x, y, z, spriteProvider);
			if (parameters instanceof LegacyFireflyParticleType fireflyParameters && fireflyParameters.initialData != null) {
				int color = fireflyParameters.initialData.color;

				float r = (float) (color >> 16 & 0xFF) / 255.0f;
				float g = (float) (color >> 8 & 0xFF) / 255.0f;
				float b = (float) (color & 0xFF) / 255.0f;

				instance.colorRed = r;
				instance.colorGreen = g;
				instance.colorBlue = b;
			}
			return instance;
		}
	}
}
