package technology.rocketjump.mountaincore.misc;

public class SteamUtils {

	public static com.codedisaster.steamworks.SteamUtils utils;
	static {
		utils = new com.codedisaster.steamworks.SteamUtils(() -> {});
	}

	public static boolean isRunningOnSteamDeck() {
		return utils.isSteamRunningOnSteamDeck();
	}

}
