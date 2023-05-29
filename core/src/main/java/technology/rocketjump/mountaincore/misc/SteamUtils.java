package technology.rocketjump.mountaincore.misc;

import com.codedisaster.steamworks.SteamAPI;

public class SteamUtils {

	public static com.codedisaster.steamworks.SteamUtils utils;
	static {
		try {
			utils = new com.codedisaster.steamworks.SteamUtils(() -> {
			});
		} catch (Throwable ignored) {}
	}

	public static boolean isRunningOnSteamDeck() {
		return SteamAPI.isSteamRunning() && utils != null && utils.isSteamRunningOnSteamDeck();
	}

}
