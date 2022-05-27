package technology.rocketjump.saul.screens;

import com.badlogic.gdx.Screen;
import technology.rocketjump.saul.ui.widgets.GameDialog;

public interface GameScreen extends Screen {

	String getName();

	void showDialog(GameDialog dialog);

}
