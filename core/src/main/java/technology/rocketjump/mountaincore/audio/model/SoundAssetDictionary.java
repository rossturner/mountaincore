package technology.rocketjump.mountaincore.audio.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.mountaincore.settlement.notifications.NotificationType;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class SoundAssetDictionary {

	private final Map<String, SoundAsset> byName = new HashMap<>();


	public static SoundAsset NULL_SOUND_ASSET = new SoundAsset() {
		@Override
		public String getName() {
			return null;
		}

		@Override
		public String toString() {
			return "-none-";
		}
	};

	@Inject
	public SoundAssetDictionary() {
		File assetDefinitionsFile = new File("assets/sounds/soundAssets.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<SoundAsset> assetList = objectMapper.readValue(FileUtils.readFileToString(assetDefinitionsFile),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, SoundAsset.class));

			for (SoundAsset asset : assetList) {
				init(asset);
				byName.put(asset.getName(), asset);
			}
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}

		for (NotificationType notificationType : NotificationType.values()) {
			if (notificationType.getOverrideSoundAssetName() != null) {
				notificationType.setOverrideSoundAsset(getByName(notificationType.getOverrideSoundAssetName()));
				if (notificationType.getOverrideSoundAsset() == null) {
					Logger.error("Could not find sound asset named " + notificationType.getOverrideSoundAssetName() + " for notification " + notificationType.name());
				}
			}
		}

		WeaponInfo.UNARMED.setWeaponHitSoundAsset(getByName("SFX_Punch_Impact"));
		if (WeaponInfo.UNARMED.getWeaponHitSoundAsset() == null) {
			Logger.error("Could not find sound asset named SFX_Punch_Impact for UNARMED default weapon info");
		}
		WeaponInfo.UNARMED.setWeaponMissSoundAsset(getByName("SFX_Melee_Miss"));
		if (WeaponInfo.UNARMED.getWeaponMissSoundAsset() == null) {
			Logger.error("Could not find sound asset named SFX_Melee_Miss for UNARMED default weapon info");
		}
	}

	private void init(SoundAsset asset) {
		List<String> qualifiedFilenames = new ArrayList<>();
		for (String unqualifiedFilename : asset.getFilenames()) {
			String qualifiedFilename = "assets/sounds/data/" + unqualifiedFilename;
			if (new File(qualifiedFilename).exists()) {
				qualifiedFilenames.add(qualifiedFilename);
			} else {
				Logger.error("Could not find expected sound file at " + qualifiedFilename);
			}
		}
		asset.setFilenames(qualifiedFilenames);
	}

	public Collection<SoundAsset> getAll() {
		return byName.values();
	}

	public SoundAsset getByName(String name) {
		return byName.get(name);
	}
}
