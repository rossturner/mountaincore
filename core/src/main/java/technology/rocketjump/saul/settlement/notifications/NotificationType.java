package technology.rocketjump.saul.settlement.notifications;

import technology.rocketjump.saul.audio.model.SoundAsset;

public enum NotificationType {

	IMMIGRANTS_ARRIVED("NOTIF_NEW_ARRIVALS.png", null),
	AREA_REVEALED("cavern_uncovered.png", null),
	MINING_COLLAPSE("cavern_collapse.png", "MiningCollapse"),
	ROOFING_COLLAPSE("cavern_collapse.png", "MiningCollapse"),
	SETTLER_TANTRUM("NOTIF_TANTRUM.png", null),
	SETTLER_MENTAL_BREAK("NOTIF_MENTAL_BREAK.png", null),
	DEATH("death.png", "Body Drop"),
	FIRE_STARTED("NOTIF_FIRE.png", null),
	OXIDISATION_DESTRUCTION("rainfall.png", null),
	FISH_EXHAUSTED("NOTIF_FISHING.png", null),
	INVASION("orc-invasion-placeholder.png", null),
	GAME_OVER("settlement-game-over.png", null);


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
