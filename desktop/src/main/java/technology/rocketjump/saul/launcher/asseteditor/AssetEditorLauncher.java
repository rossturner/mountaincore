package technology.rocketjump.saul.launcher.asseteditor;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import net.spookygames.gdx.nativefilechooser.desktop.DesktopFileChooser;
import org.lwjgl.opengl.Display;
import technology.rocketjump.saul.assets.editor.AssetEditorApplication;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.persistence.FileUtils;

public class AssetEditorLauncher {

	public static void main(String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "Asset Editor";

		Graphics.DisplayMode desktopMode = LwjglApplicationConfiguration.getDesktopDisplayMode();
		config.width = desktopMode.width - 300;
		config.height = desktopMode.height - 280;

		config.addIcon("assets/icon/editor-tool-icon-128x128.png", Files.FileType.Internal);
		config.addIcon("assets/icon/editor-tool-icon-32x32.png", Files.FileType.Internal);
		config.addIcon("assets/icon/editor-tool-icon-16x16.png", Files.FileType.Internal);

		AbstractModule launcherModule = new AbstractModule() {
			@Override
			protected void configure() {
				bind(NativeFileChooser.class).to(DesktopFileChooser.class);
			}
		};
		AssetEditorApplication gameInstance = Guice.createInjector(launcherModule).getInstance(AssetEditorApplication.class);
		new LwjglApplication(gameInstance, config) {
			@Override
			protected void mainLoop() {
				try {
					super.mainLoop();
				} catch (Throwable e) {
					Display.destroy(); //bug fix - without this, ApplicationShutdownHooks can hang indefinitely
					FileUtils.delete(EditorStateProvider.STATE_FILE);
					throw e;
				}
			}
		};
	}

}
