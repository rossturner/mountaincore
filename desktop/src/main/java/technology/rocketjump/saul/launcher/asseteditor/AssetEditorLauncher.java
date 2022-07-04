package technology.rocketjump.saul.launcher.asseteditor;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.google.inject.Guice;
import technology.rocketjump.saul.AssetsPackager;
import technology.rocketjump.saul.assets.editor.AssetEditorApplication;

import java.io.IOException;

public class AssetEditorLauncher {

	public static void main(String[] arg) throws IOException {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "Asset Editor";

		Graphics.DisplayMode desktopMode = LwjglApplicationConfiguration.getDesktopDisplayMode();
		config.width = desktopMode.width - 100;
		config.height = desktopMode.height - 80;

		// On launch repackage assets into relevant folders
		AssetsPackager.main();

		AssetEditorApplication gameInstance = Guice.createInjector().getInstance(AssetEditorApplication.class);
		new LwjglApplication(gameInstance, config);
	}

}
