package technology.rocketjump.saul.particles.custom_libgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.rendering.RenderMode;
import technology.rocketjump.saul.rendering.custom_libgdx.CustomShaderSpriteBatch;

public class ProgressBarEffect implements ParticleEffect {

	private final Vector2 worldPosition = new Vector2();
	private final Sprite progressBarInnerSprite;
	private final Sprite progressBarOuterSprite;
	private float progress; // 0 to 1
	private boolean overrideCompletion;

	private static final float WORLD_WIDTH = 0.8f;
	private static final float WORLD_HEIGHT = 0.2f;

	private static final float outerSpritePixelWidth = 190f;
	private static final float outerSpritePixelHeight = 49f;
	private static final float innnerSpritePixelWidth = 178f;
	private static final float innnerSpritePixelHeight = 33f;
	private static final float innerSpritePixelOffsetX = 6f;
	private static final float innerSpritePixelTopOffsetY = 6f;
	private static final float innerSpritePixelBottomOffsetY = 10f;

	public ProgressBarEffect(Sprite progressBarInnerSprite, Sprite progressBarOuterSprite) {
		this.progressBarInnerSprite = progressBarInnerSprite;
		this.progressBarOuterSprite = progressBarOuterSprite;
	}

	@Override
	public void update(float deltaTime) {

	}

	public void setProgress(float progress) {
		this.progress = progress;
	}

	@Override
	public boolean isComplete() {
		return progress >= 1 || overrideCompletion;
	}

	@Override
	public void allowCompletion() {
		overrideCompletion = true;
	}

	@Override
	public void draw(SpriteBatch basicSpriteBatch, CustomShaderSpriteBatch customShaderSpriteBatch, RenderMode renderMode) {
		float outerSpriteWorldX = worldPosition.x - (WORLD_WIDTH/2f);
		float outerSpriteWorldY = worldPosition.y - (WORLD_HEIGHT/2f);

		float innerSpriteWorldX = outerSpriteWorldX + ((innerSpritePixelOffsetX/outerSpritePixelWidth) * WORLD_WIDTH);
		float innerSpriteWorldY = outerSpriteWorldY + ((innerSpritePixelBottomOffsetY /outerSpritePixelHeight) * WORLD_HEIGHT);

		float innerSpriteMaxWorldWidth = WORLD_WIDTH - ((2 * innerSpritePixelOffsetX / outerSpritePixelWidth) * WORLD_WIDTH);
		float innerSpriteMaxWorldHeight = WORLD_HEIGHT - (((innerSpritePixelTopOffsetY + innerSpritePixelBottomOffsetY) / outerSpritePixelHeight) * WORLD_HEIGHT);

		basicSpriteBatch.draw(progressBarOuterSprite, outerSpriteWorldX, outerSpriteWorldY, WORLD_WIDTH, WORLD_HEIGHT);
		basicSpriteBatch.draw(progressBarInnerSprite, innerSpriteWorldX, innerSpriteWorldY, progress * innerSpriteMaxWorldWidth, innerSpriteMaxWorldHeight);
	}

	@Override
	public void setPosition(float worldX, float worldY) {
		this.worldPosition.set(worldX, worldY);
	}

	@Override
	public void setTint(Color color) {

	}

	@Override
	public void adjustForParentOrientation(EntityAssetOrientation effectDefaultOrientation, EntityAssetOrientation parentOrientation) {
		// Do nothing
	}
}
