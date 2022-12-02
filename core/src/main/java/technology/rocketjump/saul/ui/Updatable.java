package technology.rocketjump.saul.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;

import java.util.ArrayList;
import java.util.List;

public class Updatable<T extends Actor> {
	private final T actor;
	private final List<Runnable> updateCalls;

	private Updatable(T actor) {
		this.actor = actor;
		this.updateCalls = new ArrayList<>();
	}

	public static <T extends Actor> Updatable<T> of(T actor) {
		return new Updatable<>(actor);
	}

	public T getActor() {
		return actor;
	}

	public void update() {
		for (Runnable updateCall : updateCalls) {
			updateCall.run();
		}
	}

	public void regularly(Runnable updateCall) {
		this.updateCalls.add(updateCall);
	}
}
