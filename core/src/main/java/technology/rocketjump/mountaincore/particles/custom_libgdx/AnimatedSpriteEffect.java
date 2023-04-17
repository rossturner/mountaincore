package technology.rocketjump.mountaincore.particles.custom_libgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.rendering.RenderMode;
import technology.rocketjump.mountaincore.rendering.custom_libgdx.CustomShaderSpriteBatch;

import static technology.rocketjump.mountaincore.rendering.entities.EntityRenderer.PIXELS_PER_TILE;
import static technology.rocketjump.mountaincore.rendering.entities.EntityRenderer.selectFrame;

public class AnimatedSpriteEffect implements ParticleEffect {

	private final Array<Sprite> sprites;
	private final float duration;
	private final float scale;
	private final boolean isLooping;
	private float elapsed = 0;
	private final Vector2 worldPosition = new Vector2();
	private float rotation = 0;
	private Color tint = Color.WHITE;
	private boolean forceCompletion;

	public AnimatedSpriteEffect(Array<Sprite> sprites, float duration, float scale, boolean isLooping) {
		this.sprites = sprites;
		this.duration = duration;
		this.scale = scale;
		this.isLooping = isLooping;
	}

	@Override
	public void update(float deltaTime) {
		elapsed += deltaTime;
		if (elapsed > duration) {
			if (isLooping) {
				elapsed = elapsed % duration;
			} else {
				forceCompletion = true;
			}
		}
	}

	@Override
	public boolean isComplete() {
		return forceCompletion;
	}

	@Override
	public void allowCompletion() {
		forceCompletion = true;
	}

	@Override
	public void draw(SpriteBatch basicSpriteBatch, CustomShaderSpriteBatch customShaderSpriteBatch, RenderMode renderMode) {
		if (renderMode.equals(RenderMode.DIFFUSE)) {
			Sprite sprite = sprites.get(selectFrame(sprites, elapsed / duration));
			basicSpriteBatch.setColor(tint);

			Affine2 transformation = new Affine2();

			float spriteWorldWidth = (sprite.getWidth() / PIXELS_PER_TILE) * scale;
			float spriteWorldHeight = (sprite.getHeight() / PIXELS_PER_TILE) * scale;

			transformation.translate(worldPosition);
			transformation.rotate(rotation);
			transformation.translate(-spriteWorldWidth /2f, -spriteWorldHeight /2f);
			basicSpriteBatch.draw(sprite, spriteWorldWidth, spriteWorldHeight, transformation);
			basicSpriteBatch.setColor(Color.WHITE);
		} else {
			Logger.warn("Attempting to render " + getClass().getSimpleName() + " in " + renderMode.name() + " mode");
		}
	}

	@Override
	public void setPosition(float worldX, float worldY) {
		worldPosition.x = worldX;
		worldPosition.y = worldY;
	}

	@Override
	public void setTint(Color color) {
		tint = color;
	}

	@Override
	public void adjustForParentOrientation(EntityAssetOrientation effectDefaultOrientation, EntityAssetOrientation parentOrientation) {
		rotation = effectDefaultOrientation.toVector2().angleDeg(parentOrientation.toVector2());
	}
}
