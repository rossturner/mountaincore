package technology.rocketjump.saul.particles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.saul.particles.model.ParticleEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class ParticleEffectTypeDictionary {

	private final Map<String, ParticleEffectType> byName = new HashMap<>();

	@Inject
	public ParticleEffectTypeDictionary() throws IOException {
		this(new File("assets/definitions/types/particleTypes.json"));
	}

	public ParticleEffectTypeDictionary(File particleEffectTypesJsonFile) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<ParticleEffectType> particleEffectTypes = objectMapper.readValue(FileUtils.readFileToString(particleEffectTypesJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, ParticleEffectType.class));

		for (ParticleEffectType particleEffectType : particleEffectTypes) {
			byName.put(particleEffectType.getName(), particleEffectType);
		}

		ParticleEffectType clawSlash = byName.get("Claw slash");
		if (clawSlash == null) {
			Logger.error("Could not find particle effect with name `Claw slash` for UNARMED default weapon info");
		} else {
			WeaponInfo.UNARMED.setAnimatedEffectType(clawSlash);
		}

	}

	public ParticleEffectType getByName(String particleEffectName) {
		return byName.get(particleEffectName);
	}

	public Collection<ParticleEffectType> getAll() {
		return byName.values();
	}

}
