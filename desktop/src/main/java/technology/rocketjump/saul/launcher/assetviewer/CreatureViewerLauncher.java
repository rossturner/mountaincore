package technology.rocketjump.saul.launcher.assetviewer;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.google.inject.Guice;
import technology.rocketjump.saul.AssetsPackager;
import technology.rocketjump.saul.assets.viewer.CreatureViewerApplication;

import java.io.IOException;

public class CreatureViewerLauncher {

	public static void main(String[] arg) throws IOException {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Creature Viewer";
        config.width = 1280;
        config.height = 900;

		// On launch repackage assets into relevant folders
		AssetsPackager.main();


		CreatureViewerApplication gameInstance = Guice.createInjector().getInstance(CreatureViewerApplication.class);
        new LwjglApplication(gameInstance, config);
	}

}
