package technology.rocketjump.mountaincore.launcher;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.MountaincoreApplicationAdapter;
import technology.rocketjump.mountaincore.guice.MountaincoreGuiceModule;
import technology.rocketjump.mountaincore.logging.CrashHandler;
import technology.rocketjump.mountaincore.modding.LocalModRepository;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.rendering.camera.DisplaySettings;
import technology.rocketjump.mountaincore.screens.menus.Resolution;

import java.nio.charset.Charset;

import static technology.rocketjump.mountaincore.persistence.UserPreferences.FullscreenMode.BORDERLESS_FULLSCREEN;
import static technology.rocketjump.mountaincore.persistence.UserPreferences.FullscreenMode.WINDOWED;
import static technology.rocketjump.mountaincore.persistence.UserPreferences.PreferenceKey.DISPLAY_RESOLUTION;
import static technology.rocketjump.mountaincore.screens.menus.options.GraphicsOptionsTab.getFullscreenMode;

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


        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        config.setTitle("Mountaincore");

        Injector preInjector = Guice.createInjector(new MountaincoreGuiceModule());
        UserPreferences userPreferences = preInjector.getInstance(UserPreferences.class);

        LocalModRepository localModRepository = preInjector.getInstance(LocalModRepository.class);
        localModRepository.packageActiveMods();

        UserPreferences.FullscreenMode fullscreenMode = getFullscreenMode(userPreferences);
        Resolution displayResolution = getDisplayResolution(userPreferences);
        if (fullscreenMode == WINDOWED || fullscreenMode == BORDERLESS_FULLSCREEN) {
            config.setWindowedMode(displayResolution.width, displayResolution.height);
            if (fullscreenMode == BORDERLESS_FULLSCREEN) {
                config.setDecorated(false);
            }
        } else {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        }

        config.setWindowIcon("assets/icon/Steam_Icon_128x128.png", "assets/icon/Steam_Icon_32x32.png", "assets/icon/Steam_Icon_16x16.png");


        MountaincoreApplicationAdapter gameInstance = new MountaincoreApplicationAdapter();
        new Lwjgl3Application(gameInstance, config);
    }


    private static Resolution getDisplayResolution(UserPreferences userPreferences) {
        Graphics.DisplayMode desktopMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
        Resolution desktopResolution = new Resolution(desktopMode.width, desktopMode.height);
        String preferredResolution = userPreferences.getPreference(DISPLAY_RESOLUTION);
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
