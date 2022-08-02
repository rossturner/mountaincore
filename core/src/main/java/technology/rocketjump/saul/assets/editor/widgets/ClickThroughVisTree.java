package technology.rocketjump.saul.assets.editor.widgets;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisTree;

public class ClickThroughVisTree extends VisTree {

	public ClickThroughVisTree() {
		ClickThroughVisTree This = this;
		// This listener fakes a click event onto the actor of a node on a row that has been clicked
		this.addListener(new PassThroughClickListener(this, Input.Buttons.LEFT));
		this.addListener(new PassThroughClickListener(this, Input.Buttons.RIGHT));
	}

	private static class PassThroughClickListener extends ClickListener {

		private final ClickThroughVisTree parent;

		public PassThroughClickListener(ClickThroughVisTree parent, int button) {
			super(button);
			this.parent = parent;
		}

		@Override
		public void clicked(InputEvent event, float x, float y) {
			super.clicked(event, x, y);
			if (parent.equals(event.getTarget())) {
				Node node = parent.getNodeAt(y);
				if (node != null) {
					InputEvent touchDown = new InputEvent();
					touchDown.setType(InputEvent.Type.touchDown);
					touchDown.setButton(this.getButton());
					node.getActor().fire(touchDown);

					InputEvent touchUp = new InputEvent();
					touchUp.setType(InputEvent.Type.touchUp);
					touchUp.setButton(this.getButton());
					node.getActor().fire(touchUp);
				}
			}
		}
	}

}
