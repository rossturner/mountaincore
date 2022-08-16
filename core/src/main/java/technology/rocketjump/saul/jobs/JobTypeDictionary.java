package technology.rocketjump.saul.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.saul.jobs.model.JobType;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.particles.model.ParticleEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static technology.rocketjump.saul.jobs.SkillDictionary.CONTEXT_DEPENDENT_PROFESSION_REQUIRED;
import static technology.rocketjump.saul.jobs.SkillDictionary.NULL_PROFESSION;

@Singleton
public class JobTypeDictionary {

	private final ItemTypeDictionary itemTypeDictionary;
	private final SkillDictionary skillDictionary;
	private final SoundAssetDictionary soundAssetDictionary;
	private final ParticleEffectTypeDictionary particleEffectTypeDictionary;

	private final Map<String, JobType> byName = new HashMap<>();

	@Inject
	public JobTypeDictionary(ItemTypeDictionary itemTypeDictionary, SkillDictionary skillDictionary,
							 SoundAssetDictionary soundAssetDictionary, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.itemTypeDictionary = itemTypeDictionary;
		this.skillDictionary = skillDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;

		File assetDefinitionsFile = new File("assets/definitions/types/jobTypes.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<JobType> jobTypeList = objectMapper.readValue(FileUtils.readFileToString(assetDefinitionsFile),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, JobType.class));

			for (JobType jobType : jobTypeList) {
				init(jobType);
				byName.put(jobType.getName(), jobType);
			}
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}

		for (PlantSpeciesType plantSpeciesType : PlantSpeciesType.values()) {
			plantSpeciesType.setRemovalJobType(byName.get(plantSpeciesType.removalJobTypeName));
			if (plantSpeciesType.getRemovalJobType() == null) {
				Logger.error("Could not find required removalJobType " + plantSpeciesType.removalJobTypeName + " for " + plantSpeciesType.name());
			}
		}

	}

	public JobType getByName(String jobTypeName) {
		return byName.get(jobTypeName);
	}

	public Collection<JobType> getAll() {
		return byName.values();
	}

	private void init(JobType jobType) {
		if (jobType.getRequiredProfessionName() != null) {
			if (jobType.getRequiredProfessionName().equals("CONTEXT_DEPENDENT_PROFESSION_REQUIRED")) {
				jobType.setRequiredProfession(CONTEXT_DEPENDENT_PROFESSION_REQUIRED);
			} else {
				jobType.setRequiredProfession(skillDictionary.getByName(jobType.getRequiredProfessionName()));
				if (jobType.getRequiredProfession() == null) {
					Logger.error("Could not profession with name " + jobType.getRequiredProfessionName() + " for job type " + jobType.getName());
				}
			}
		} else {
			jobType.setRequiredProfession(NULL_PROFESSION);
		}

		if (jobType.getRequiredItemTypeName() != null) {
			jobType.setRequiredItemType(itemTypeDictionary.getByName(jobType.getRequiredItemTypeName()));
			if (jobType.getRequiredItemType() == null) {
				Logger.error("Could not find item type with name " + jobType.getRequiredItemTypeName() + " for job type " + jobType.getName());
			}
		}
		if (jobType.getActiveSoundAssetName() != null) {
			jobType.setActiveSoundAsset(soundAssetDictionary.getByName(jobType.getActiveSoundAssetName()));
			if (jobType.getActiveSoundAsset() == null) {
				Logger.error("Could not find sound asset with name " + jobType.getActiveSoundAssetName() + " for job type " + jobType.getName());
			}
		}
		if (jobType.getOnCompletionSoundAssetName() != null) {
			jobType.setOnCompletionSoundAsset(soundAssetDictionary.getByName(jobType.getOnCompletionSoundAssetName()));
			if (jobType.getOnCompletionSoundAsset() == null) {
				Logger.error("Could not find sound asset with name " + jobType.getOnCompletionSoundAssetName() + " for job type " + jobType.getName());
			}
		}
		if (jobType.getWorkOnJobParticleEffectNames() != null) {
			for (String workOnJobParticleEffectName : jobType.getWorkOnJobParticleEffectNames()) {
				ParticleEffectType particleEffectType = particleEffectTypeDictionary.getByName(workOnJobParticleEffectName);
				if (particleEffectType == null) {
					Logger.error("Could not find particle effect with name " + workOnJobParticleEffectName + " for job type " + jobType.getName());
				} else {
					jobType.getWorkOnJobParticleEffectTypes().add(particleEffectType);
				}
			}
		}
	}
}
