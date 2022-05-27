package technology.rocketjump.saul.particles.custom_libgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.rendering.RenderMode;
import technology.rocketjump.saul.rendering.custom_libgdx.CustomShaderSpriteBatch;

public interface ParticleEffect {

	void update(float deltaTime);

	boolean isComplete();

	void allowCompletion();

	void draw(SpriteBatch basicSpriteBatch, CustomShaderSpriteBatch customShaderSpriteBatch, RenderMode renderMode);

	void setPosition(float worldX, float worldY);

	void setTint(Color color);

	void adjustForParentOrientation(EntityAssetOrientation effectDefaultOrientation, EntityAssetOrientation parentOrientation);
}
