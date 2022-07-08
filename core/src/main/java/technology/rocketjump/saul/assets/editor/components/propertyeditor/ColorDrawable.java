package technology.rocketjump.saul.assets.editor.components.propertyeditor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;

import static technology.rocketjump.saul.assets.editor.components.propertyeditor.ComponentBuilder.staticShapeRenderer;

public class ColorDrawable extends BaseDrawable {

	private Color color;

	public ColorDrawable(Color color) {
		this.color = color;
	}

	@Override
	public void draw(Batch batch, float x, float y, float width, float height) {
		staticShapeRenderer.setColor(color);
		staticShapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		staticShapeRenderer.rect(x, y, width, height);
		staticShapeRenderer.end();
	}

	public void setColor(Color color) {
		this.color = color;
	}
}
