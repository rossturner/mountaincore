package technology.rocketjump.saul.launcher.asseteditor;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import net.spookygames.gdx.nativefilechooser.desktop.DesktopFileChooser;
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

		AbstractModule launcherModule = new AbstractModule() {
			@Override
			protected void configure() {
				bind(NativeFileChooser.class).to(DesktopFileChooser.class);
			}
		};
		AssetEditorApplication gameInstance = Guice.createInjector(launcherModule).getInstance(AssetEditorApplication.class);
		new LwjglApplication(gameInstance, config);
	}

}
