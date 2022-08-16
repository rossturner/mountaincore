package technology.rocketjump.saul.particles.custom_libgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.rendering.RenderMode;
import technology.rocketjump.saul.rendering.custom_libgdx.CustomShaderSpriteBatch;

import static technology.rocketjump.saul.rendering.entities.EntityRenderer.PIXELS_PER_TILE;
import static technology.rocketjump.saul.rendering.entities.EntityRenderer.selectFrame;

public class AnimatedSpriteEffect implements ParticleEffect {

	private final Array<Sprite> sprites;
	private final float duration;
	private float elapsed = 0;
	private final Vector2 worldPosition = new Vector2();
	private float rotation = 0;
	private Color tint = Color.WHITE;

	public AnimatedSpriteEffect(Array<Sprite> sprites, float duration) {
		this.sprites = sprites;
		this.duration = duration;
	}

	@Override
	public void update(float deltaTime) {
		elapsed += deltaTime;
	}

	@Override
	public boolean isComplete() {
		return elapsed >= duration;
	}

	@Override
	public void allowCompletion() {
		elapsed = duration;
	}

	@Override
	public void draw(SpriteBatch basicSpriteBatch, CustomShaderSpriteBatch customShaderSpriteBatch, RenderMode renderMode) {
		if (renderMode.equals(RenderMode.DIFFUSE)) {
			Sprite sprite = sprites.get(selectFrame(sprites, elapsed / duration));
			basicSpriteBatch.setColor(tint);

			Affine2 transformation = new Affine2();

			float spriteWorldWidth = sprite.getWidth() / PIXELS_PER_TILE;
			float spriteWorldHeight = sprite.getHeight() / PIXELS_PER_TILE;

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
		rotation = effectDefaultOrientation.toVector2().angle(parentOrientation.toVector2());
	}
}
