package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
This is a scrollpane that includes default behaviour like scroll focus on mouse hover
 */
public class EnhancedScrollPane extends ScrollPane {
	public EnhancedScrollPane(Actor widget, Skin skin) {
		super(widget, skin);

		addListener(new InputListener() {
			@Override
			public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
				super.enter(event, x, y, pointer, fromActor);
				if (getStage() != null) {
					getStage().setScrollFocus(EnhancedScrollPane.this);
				}
			}

			@Override
			public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				super.exit(event, x, y, pointer, toActor);
				if (getStage() != null && pointer == -1) {
					Vector2 localCoords = EnhancedScrollPane.this.screenToLocalCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
					// only actually exit when no longer over scroll pane
					if (localCoords.x < 0 || localCoords.x > getHeight() || localCoords.y < 0 || localCoords.y > getWidth()) {
						getStage().setScrollFocus(null);
					}
				}
			}
		});
	}
}
