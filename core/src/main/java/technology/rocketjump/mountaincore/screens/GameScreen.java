package technology.rocketjump.mountaincore.screens;

import com.badlogic.gdx.Screen;
import technology.rocketjump.mountaincore.ui.widgets.GameDialog;

public interface GameScreen extends Screen {

	String getName();

	void showDialog(GameDialog dialog);

}
