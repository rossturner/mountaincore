package technology.rocketjump.mountaincore.launcher.asseteditor;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import net.spookygames.gdx.nativefilechooser.desktop.DesktopFileChooser;
import technology.rocketjump.mountaincore.AssetsPackager;
import technology.rocketjump.mountaincore.assets.editor.AssetEditorApplication;
import technology.rocketjump.mountaincore.assets.editor.model.EditorStateProvider;
import technology.rocketjump.mountaincore.persistence.FileUtils;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;

public class AssetEditorLauncher {

	public static void main(String[] arg) {
		Graphics.DisplayMode desktopMode = Lwjgl3ApplicationConfiguration.getDisplayMode();

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Asset Editor");
		config.setWindowedMode(desktopMode.width - 300, desktopMode.height - 280);
		config.setWindowIcon("assets/icon/editor-tool-icon-128x128.png", "assets/icon/editor-tool-icon-32x32.png", "assets/icon/editor-tool-icon-16x16.png");

		if (GlobalSettings.DEV_MODE) {
			AssetsPackager.main();
		}

		AbstractModule launcherModule = new AbstractModule() {
			@Override
			protected void configure() {
				bind(NativeFileChooser.class).to(DesktopFileChooser.class);
			}
		};
		AssetEditorApplication gameInstance = Guice.createInjector(launcherModule).getInstance(AssetEditorApplication.class);
		new Lwjgl3Application(gameInstance, config) {
			@Override
			protected void loop() {
				try {
					super.loop();
				} catch (Throwable e) {
					FileUtils.delete(EditorStateProvider.STATE_FILE);
					throw e;
				}
			}
		};
	}

}
