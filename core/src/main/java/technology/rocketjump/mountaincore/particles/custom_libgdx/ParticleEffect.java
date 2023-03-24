package technology.rocketjump.mountaincore.particles.custom_libgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.rendering.RenderMode;
import technology.rocketjump.mountaincore.rendering.custom_libgdx.CustomShaderSpriteBatch;

public interface ParticleEffect {

	void update(float deltaTime);

	boolean isComplete();

	void allowCompletion();

	void draw(SpriteBatch basicSpriteBatch, CustomShaderSpriteBatch customShaderSpriteBatch, RenderMode renderMode);

	void setPosition(float worldX, float worldY);

	void setTint(Color color);

	void adjustForParentOrientation(EntityAssetOrientation effectDefaultOrientation, EntityAssetOrientation parentOrientation);
}
