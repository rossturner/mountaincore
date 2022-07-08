package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;
import technology.rocketjump.saul.assets.entities.model.StorableVector2;

public class OffsetPixelsWidget extends VisTable {


	public OffsetPixelsWidget(StorableVector2 sourceVector) {
		this.add(new VisLabel("Offset:")).left();

		this.add(new VisLabel("x")).right();
		VisTextField xOffsetField = new VisTextField(String.valueOf(sourceVector.getX()));
		xOffsetField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				try {
					Float newValue = Float.valueOf(xOffsetField.getText());
					if (newValue != null) {
						sourceVector.setX(newValue);
					}
				} catch (NumberFormatException e) {

				}
			}
		});
		this.add(xOffsetField).expandX().fillX().left();

		this.add(new VisLabel("y")).right();
		VisTextField yOffsetField = new VisTextField(String.valueOf(sourceVector.getY()));
		yOffsetField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				try {
					Float newValue = Float.valueOf(yOffsetField.getText());
					if (newValue != null) {
						sourceVector.setY(newValue);
					}
				} catch (NumberFormatException e) {

				}
			}
		});
		this.add(yOffsetField).expandX().fillX().left();
	}
}
