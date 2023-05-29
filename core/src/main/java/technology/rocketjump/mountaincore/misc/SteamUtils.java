package technology.rocketjump.mountaincore.misc;

import com.codedisaster.steamworks.SteamAPI;
import org.apache.commons.lang3.SystemUtils;

public class SteamUtils {

	public static com.codedisaster.steamworks.SteamUtils utils;
	static {
		try {
			if (!SystemUtils.IS_OS_MAC) {
				utils = new com.codedisaster.steamworks.SteamUtils(() -> {});
			}
		} catch (Throwable ignored) {}
	}

	public static boolean isRunningOnSteamDeck() {
		return utils != null && SteamAPI.isSteamRunning() && utils.isSteamRunningOnSteamDeck();
	}

}
