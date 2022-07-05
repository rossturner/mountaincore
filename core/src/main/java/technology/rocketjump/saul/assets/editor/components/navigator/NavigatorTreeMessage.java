package technology.rocketjump.saul.assets.editor.components.navigator;

import com.badlogic.gdx.scenes.scene2d.Actor;

public class NavigatorTreeMessage {

	public final NavigatorTreeValue value;
	public final Actor actor;

	public NavigatorTreeMessage(NavigatorTreeValue value, Actor actor) {
		this.value = value;
		this.actor = actor;
	}

}
