package technology.rocketjump.saul.settlement.notifications;

import technology.rocketjump.saul.audio.model.SoundAsset;

public enum NotificationType {

	IMMIGRANTS_ARRIVED("NOTIF_NEW_ARRIVALS.png", null),
	AREA_REVEALED("NOTIF_CAVE_DISCOVERED.png", null),
	MINING_COLLAPSE("NOTIF_COLLAPSE.png", "MiningCollapse"),
	ROOFING_COLLAPSE("NOTIF_COLLAPSE.png", "MiningCollapse"),
	SETTLER_TANTRUM("NOTIF_TANTRUM.png", null),
	SETTLER_MENTAL_BREAK("NOTIF_MENTAL_BREAK.png", null),
	DEATH("NOTIF_DIED.png", "Body Drop"),
	FIRE_STARTED("NOTIF_FIRE.png", null),
	OXIDISATION_DESTRUCTION("NOTIF_ITEM_DESTROYED.png", null),
	FISH_EXHAUSTED("NOTIF_FISHING.png", null),
	INVASION("NOTIF_INVASION.png", null),
	GAME_OVER("NOTIF_GAME_OVER.png", null);


	private String imageFilename;
	private String overrideSoundAssetName;
	private SoundAsset overrideSoundAsset;

	NotificationType(String imageFileName, String overrideSoundAssetName) {
		this.imageFilename = imageFileName;
		this.overrideSoundAssetName = overrideSoundAssetName;
	}

	public String getI18nTitleKey() {
		return "NOTIFICATION."+name()+".TITLE";
	}

	public String getI18nDescriptionKey() {
		return "NOTIFICATION."+name()+".DESCRIPTION";
	}

	public String getImageFilename() {
		return imageFilename;
	}

	public String getOverrideSoundAssetName() {
		return overrideSoundAssetName;
	}

	public SoundAsset getOverrideSoundAsset() {
		return overrideSoundAsset;
	}

	public void setOverrideSoundAsset(SoundAsset overrideSoundAsset) {
		this.overrideSoundAsset = overrideSoundAsset;
	}
}
