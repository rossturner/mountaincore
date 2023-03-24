package technology.rocketjump.mountaincore.particles.custom_libgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.rendering.RenderMode;
import technology.rocketjump.mountaincore.rendering.custom_libgdx.CustomShaderSpriteBatch;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;

public class ProgressBarEffect implements ParticleEffect {

	private final Vector2 worldPosition = new Vector2();
	private final Sprite progressBarInnerSprite;
	private final Sprite progressBarOuterSprite;
	private float progress; // 0 to 1
	private boolean overrideCompletion;

	private static final float WORLD_WIDTH = 0.8f;
	private static final float WORLD_HEIGHT = 0.2f;
	private static final Color SLIGHTLY_TRANSPARENT_WHITE = HexColors.get("#FFFFFFDD");

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
		Color originalColor = basicSpriteBatch.getColor();
		basicSpriteBatch.setColor(SLIGHTLY_TRANSPARENT_WHITE);

		float spriteWorldX = worldPosition.x - (WORLD_WIDTH/2f);
		float spriteWorldY = worldPosition.y - (WORLD_HEIGHT/2f);
		float progressWidth = progress * WORLD_WIDTH;

		basicSpriteBatch.draw(progressBarOuterSprite, spriteWorldX + progressWidth, spriteWorldY, WORLD_WIDTH - progressWidth, WORLD_HEIGHT);
		basicSpriteBatch.draw(progressBarInnerSprite, spriteWorldX, spriteWorldY, progressWidth, WORLD_HEIGHT);

		basicSpriteBatch.setColor(originalColor);
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
