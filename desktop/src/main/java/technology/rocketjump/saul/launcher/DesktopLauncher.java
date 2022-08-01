package technology.rocketjump.saul.launcher;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.SaulLwjglApplication;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.SaulApplicationAdapter;
import technology.rocketjump.saul.guice.SaulGuiceModule;
import technology.rocketjump.saul.logging.CrashHandler;
import technology.rocketjump.saul.modding.LocalModRepository;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.rendering.camera.DisplaySettings;
import technology.rocketjump.saul.screens.menus.Resolution;

import java.nio.charset.Charset;

import static technology.rocketjump.saul.persistence.UserPreferences.FullscreenMode.BORDERLESS_FULLSCREEN;
import static technology.rocketjump.saul.persistence.UserPreferences.FullscreenMode.EXCLUSIVE_FULLSCREEN;
import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.DISPLAY_RESOLUTION;
import static technology.rocketjump.saul.screens.menus.options.GraphicsOptionsTab.getFullscreenMode;

public class DesktopLauncher {

    public static void main(String[] args) {
        try {
            checkDefaultCharset();
            launchMainWindow();
        } catch (Throwable e) {
            Logger.error(e);
            CrashHandler.logCrash(e);
            System.exit(-1);
        }
    }

    private static void launchMainWindow() {
        // config for main window


        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

        config.title = "Project Saul";

        Injector preInjector = Guice.createInjector(new SaulGuiceModule());
        UserPreferences userPreferences = preInjector.getInstance(UserPreferences.class);

        LocalModRepository localModRepository = preInjector.getInstance(LocalModRepository.class);
        localModRepository.packageActiveMods();

        UserPreferences.FullscreenMode fullscreenMode = getFullscreenMode(userPreferences);

        if (fullscreenMode.equals(BORDERLESS_FULLSCREEN)) {
            System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
        }
        config.fullscreen = fullscreenMode.equals(EXCLUSIVE_FULLSCREEN);


        Resolution displayResolution = getDisplayResolution(userPreferences);
        config.width = displayResolution.width;
        config.height = displayResolution.height;

        config.addIcon("assets/icon/Steam_Icon_128x128.png", Files.FileType.Internal);
        config.addIcon("assets/icon/Steam_Icon_32x32.png", Files.FileType.Internal);
        config.addIcon("assets/icon/Steam_Icon_16x16.png", Files.FileType.Internal);

        SaulApplicationAdapter gameInstance = new SaulApplicationAdapter();
        new SaulLwjglApplication(gameInstance, config);
    }

    private static Resolution getDisplayResolution(UserPreferences userPreferences) {
        Graphics.DisplayMode desktopMode = LwjglApplicationConfiguration.getDesktopDisplayMode();
        Resolution desktopResolution = new Resolution(desktopMode.width, desktopMode.height);
        String preferredResolution = userPreferences.getPreference(DISPLAY_RESOLUTION, null);
        Resolution resolutionToUse;
        if (preferredResolution == null) {
            userPreferences.setPreference(DISPLAY_RESOLUTION, desktopResolution.toString());
            resolutionToUse = desktopResolution;
        } else {
            try {
                resolutionToUse = Resolution.byString(preferredResolution);
            } catch (NumberFormatException e) {
                Logger.error("Could not parse " + DISPLAY_RESOLUTION.name() + " preference: " + preferredResolution);
                resolutionToUse = desktopResolution;
            }
        }
        DisplaySettings.currentResolution = resolutionToUse;
        return resolutionToUse;
    }

	private static void checkDefaultCharset() {
        Logger.info("Default character set is " + Charset.defaultCharset().name());
	}

}
