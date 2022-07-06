package technology.rocketjump.saul.assets.editor.components.entitybrowser;

import com.badlogic.gdx.scenes.scene2d.Actor;

public class EntityBrowserTreeMessage {

	public final EntityBrowserValue value;
	public final Actor actor;

	public EntityBrowserTreeMessage(EntityBrowserValue value, Actor actor) {
		this.value = value;
		this.actor = actor;
	}

}
