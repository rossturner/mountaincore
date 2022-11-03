package technology.rocketjump.saul.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class KeyBindingInputProcessor extends InputAdapter {
	private final InputProcessor currentInputProcessor;
	private final Consumer<Set<Integer>> keyboardCapture;
	private final Consumer<Integer> mouseCapture;
	private final Set<Integer> keysPressed = new HashSet<>();

	public KeyBindingInputProcessor(InputProcessor currentInputProcessor, Consumer<Set<Integer>> keyboardCapture, Consumer<Integer> mouseCapture) {
		this.currentInputProcessor = currentInputProcessor;
		this.keyboardCapture = keyboardCapture;
		this.mouseCapture = mouseCapture;
	}

	@Override
	public boolean keyDown(int keycode) {
		keysPressed.add(keycode);
		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		if (keysPressed.contains(Input.Keys.ESCAPE)) {
			keyboardCapture.accept(Collections.emptySet());
		} else {
			keyboardCapture.accept(keysPressed);
		}
		Gdx.input.setInputProcessor(currentInputProcessor);
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		mouseCapture.accept(button);
		Gdx.input.setInputProcessor(currentInputProcessor);
		return true;
	}
}
