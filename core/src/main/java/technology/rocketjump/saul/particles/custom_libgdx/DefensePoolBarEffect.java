package technology.rocketjump.saul.particles.custom_libgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.rendering.RenderMode;
import technology.rocketjump.saul.rendering.custom_libgdx.CustomShaderSpriteBatch;
import technology.rocketjump.saul.rendering.utils.HexColors;

import static technology.rocketjump.saul.rendering.entities.EntityRenderer.PIXELS_PER_TILE;

public class DefensePoolBarEffect implements ParticleEffect {

	private final Vector2 worldPosition = new Vector2();
	private final Sprite shieldIconSprite;
	private final Sprite progressBarOuterSprite;
	private final Sprite progressBarInnerSprite;
	private Color shieldSpriteColor = HexColors.get("#74d7e9");
	private Color progressBarColor = HexColors.get("#80ecff");
	private float poolPercentage; // 0 to 1
	private boolean forceCompletion;

	private final float worldWidth;
	private final float worldHeight;

	private static final float innerSpritePixelOffsetX = 31f / 2f;
	private static final float innerSpritePixelMaxWidth = 115f / 2f;
	private static final float innerSpritePixelHeight = 16f / 2f;
	private static final float innerSpritePixelBottomOffsetY = 18f / 2f;

	public DefensePoolBarEffect(Sprite shieldIconSprite, Sprite progressBarInnerSprite, Sprite progressBarOuterSprite) {
		this.shieldIconSprite = shieldIconSprite;
		this.progressBarInnerSprite = progressBarInnerSprite;
		this.progressBarOuterSprite = progressBarOuterSprite;

		this.worldWidth = (shieldIconSprite.getWidth() / PIXELS_PER_TILE) / 2f;
		this.worldHeight = (shieldIconSprite.getHeight() / PIXELS_PER_TILE) / 2f;
	}

	@Override
	public void update(float deltaTime) {

	}

	public void setPoolPercentage(float poolPercentage) {
		this.poolPercentage = poolPercentage;
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
		float outerSpriteWorldX = worldPosition.x - (worldWidth /2f);
		float outerSpriteWorldY = worldPosition.y - (worldHeight /2f);

		float innerSpriteWorldX = outerSpriteWorldX + (innerSpritePixelOffsetX / PIXELS_PER_TILE);
		float innerSpriteWorldY = outerSpriteWorldY + (innerSpritePixelBottomOffsetY / PIXELS_PER_TILE);

		float innerSpriteWorldWidth = (innerSpritePixelMaxWidth / PIXELS_PER_TILE) * poolPercentage;
		float innerSpriteWorldHeight = innerSpritePixelHeight / PIXELS_PER_TILE;

		basicSpriteBatch.setColor(Color.WHITE);
		basicSpriteBatch.draw(progressBarOuterSprite, outerSpriteWorldX, outerSpriteWorldY, worldWidth, worldHeight);
		basicSpriteBatch.setColor(progressBarColor);
		basicSpriteBatch.draw(progressBarInnerSprite, innerSpriteWorldX, innerSpriteWorldY, innerSpriteWorldWidth, innerSpriteWorldHeight);
		basicSpriteBatch.setColor(shieldSpriteColor);
		basicSpriteBatch.draw(shieldIconSprite, outerSpriteWorldX, outerSpriteWorldY, worldWidth, worldHeight);
	}

	@Override
	public void setPosition(float worldX, float worldY) {
		this.worldPosition.set(worldX, worldY);
	}

	@Override
	public void setTint(Color color) {
		this.progressBarColor = color;
	}

	public void setShieldSpriteColor(Color shieldSpriteColor) {
		this.shieldSpriteColor = shieldSpriteColor;
	}

	public void setProgressBarColor(Color progressBarColor) {
		this.progressBarColor = progressBarColor;
	}

	@Override
	public void adjustForParentOrientation(EntityAssetOrientation effectDefaultOrientation, EntityAssetOrientation parentOrientation) {
		// Do nothing
	}
}
