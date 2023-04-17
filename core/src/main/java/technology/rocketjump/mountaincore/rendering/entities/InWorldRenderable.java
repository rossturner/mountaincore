package technology.rocketjump.mountaincore.rendering.entities;

import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectInstance;

import java.util.Comparator;

import static technology.rocketjump.mountaincore.entities.model.EntityType.ITEM;
import static technology.rocketjump.mountaincore.entities.model.EntityType.PLANT;

public class InWorldRenderable {

	public final Entity entity;
	public final ParticleEffectInstance particleEffect;

	public InWorldRenderable(Entity entity) {
		this.entity = entity;
		this.particleEffect = null;
	}

	public InWorldRenderable(ParticleEffectInstance particleEffect) {
		this.entity = null;
		this.particleEffect = particleEffect;
	}

	public static class YDepthEntityComparator implements Comparator<InWorldRenderable> {
		private static final float THRESHOLD = 0.001f;

		@Override
		public int compare(InWorldRenderable o1, InWorldRenderable o2) {
			float o1Position = (o1.entity != null ? o1.entity.getLocationComponent().getWorldPosition() : o1.particleEffect.getWorldPosition()).y;
			float o2Position = (o2.entity != null ? o2.entity.getLocationComponent().getWorldPosition() : o2.particleEffect.getWorldPosition()).y;


			if (o1.entity != null && (o1.entity.getType().equals(ITEM) || o1.entity.getType().equals(PLANT))) {
				o1Position += 0.5f;
			}
			if (o2.entity != null && (o2.entity.getType().equals(ITEM) || o2.entity.getType().equals(PLANT))) {
				o2Position += 0.5f;
			}

			float difference = o2Position - o1Position;
			if (Math.abs(difference) < THRESHOLD) {
				return (int) (getId(o1) - getId(o2));
			} else {
				return (int)(100000f * difference);
			}
		}

		private long getId(InWorldRenderable a) {
			if (a.entity != null) {
				return a.entity.getId();
			} else if (a.particleEffect != null) {
				return a.particleEffect.getInstanceId();
			} else {
				return 0;
			}
		}
	}

}
