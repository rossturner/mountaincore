package technology.rocketjump.saul.jobs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.entities.ai.goap.SpecialGoal;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.misc.Name;
import technology.rocketjump.saul.particles.model.ParticleEffectType;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobType {

	private static final Float DEFAULT_JOB_TIME = 4f;
	@Name
	private String name;
	private String overrideI18nKey;
	private boolean isAccessedFromAdjacentTile;
	private boolean removeJobWhenAssignmentCancelled;
	private boolean haulItemWhileWorking;
	private boolean usesWorkstationTool;
	private SpecialGoal switchToSpecialGoal;
	private Float minimumTimeToCompleteJob;
	private Float maximumTimeToCompleteJob;
	private Float mightStartFire;
	private Integer experienceAwardedOnCompletion;

	private String requiredProfessionName;
	@JsonIgnore
	private Skill requiredProfession;

	private String requiredItemTypeName;
	@JsonIgnore
	private ItemType requiredItemType;

	private String activeSoundAssetName;
	@JsonIgnore
	private SoundAsset activeSoundAsset;

	private String onCompletionSoundAssetName;
	@JsonIgnore
	private SoundAsset onCompletionSoundAsset;

	private List<String> workOnJobParticleEffectNames;
	@JsonIgnore
	private List<ParticleEffectType> workOnJobParticleEffectTypes = new ArrayList<>();
	private String workOnJobAnimation;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOverrideI18nKey() {
		return overrideI18nKey;
	}

	public void setOverrideI18nKey(String overrideI18nKey) {
		this.overrideI18nKey = overrideI18nKey;
	}

	public String getRequiredProfessionName() {
		return requiredProfessionName;
	}

	public void setRequiredProfessionName(String requiredProfessionName) {
		this.requiredProfessionName = requiredProfessionName;
	}

	public String getRequiredItemTypeName() {
		return requiredItemTypeName;
	}

	public void setRequiredItemTypeName(String requiredItemTypeName) {
		this.requiredItemTypeName = requiredItemTypeName;
	}

	public boolean isAccessedFromAdjacentTile() {
		return isAccessedFromAdjacentTile;
	}

	public void setAccessedFromAdjacentTile(boolean accessedFromAdjacentTile) {
		isAccessedFromAdjacentTile = accessedFromAdjacentTile;
	}

	public boolean isRemoveJobWhenAssignmentCancelled() {
		return removeJobWhenAssignmentCancelled;
	}

	public void setRemoveJobWhenAssignmentCancelled(boolean removeJobWhenAssignmentCancelled) {
		this.removeJobWhenAssignmentCancelled = removeJobWhenAssignmentCancelled;
	}

	public boolean isHaulItemWhileWorking() {
		return haulItemWhileWorking;
	}

	public void setHaulItemWhileWorking(boolean haulItemWhileWorking) {
		this.haulItemWhileWorking = haulItemWhileWorking;
	}

	public SpecialGoal getSwitchToSpecialGoal() {
		return switchToSpecialGoal;
	}

	public void setSwitchToSpecialGoal(SpecialGoal switchToSpecialGoal) {
		this.switchToSpecialGoal = switchToSpecialGoal;
	}

	public String getActiveSoundAssetName() {
		return activeSoundAssetName;
	}

	public void setActiveSoundAssetName(String activeSoundAssetName) {
		this.activeSoundAssetName = activeSoundAssetName;
	}

	public String getOnCompletionSoundAssetName() {
		return onCompletionSoundAssetName;
	}

	public void setOnCompletionSoundAssetName(String onCompletionSoundAssetName) {
		this.onCompletionSoundAssetName = onCompletionSoundAssetName;
	}

	public Skill getRequiredProfession() {
		return requiredProfession;
	}

	public void setRequiredProfession(Skill requiredProfession) {
		this.requiredProfession = requiredProfession;
	}

	public ItemType getRequiredItemType() {
		return requiredItemType;
	}

	public void setRequiredItemType(ItemType requiredItemType) {
		this.requiredItemType = requiredItemType;
	}

	public SoundAsset getActiveSoundAsset() {
		return activeSoundAsset;
	}

	public void setActiveSoundAsset(SoundAsset activeSoundAsset) {
		this.activeSoundAsset = activeSoundAsset;
	}

	public SoundAsset getOnCompletionSoundAsset() {
		return onCompletionSoundAsset;
	}

	public void setOnCompletionSoundAsset(SoundAsset onCompletionSoundAsset) {
		this.onCompletionSoundAsset = onCompletionSoundAsset;
	}

	public List<String> getWorkOnJobParticleEffectNames() {
		return workOnJobParticleEffectNames;
	}

	public void setWorkOnJobParticleEffectNames(List<String> workOnJobParticleEffectNames) {
		this.workOnJobParticleEffectNames = workOnJobParticleEffectNames;
	}

	public List<ParticleEffectType> getWorkOnJobParticleEffectTypes() {
		return workOnJobParticleEffectTypes;
	}

	public void setWorkOnJobParticleEffectTypes(List<ParticleEffectType> workOnJobParticleEffectTypes) {
		this.workOnJobParticleEffectTypes = workOnJobParticleEffectTypes;
	}


	public Float getMinimumTimeToCompleteJob() {
		return minimumTimeToCompleteJob != null ? minimumTimeToCompleteJob : DEFAULT_JOB_TIME;
	}

	public void setMinimumTimeToCompleteJob(Float minimumTimeToCompleteJob) {
		this.minimumTimeToCompleteJob = minimumTimeToCompleteJob;
	}

	public Float getMightStartFire() {
		return mightStartFire;
	}

	public void setMightStartFire(Float mightStartFire) {
		this.mightStartFire = mightStartFire;
	}

	public boolean isUsesWorkstationTool() {
		return usesWorkstationTool;
	}

	public void setUsesWorkstationTool(boolean usesWorkstationTool) {
		this.usesWorkstationTool = usesWorkstationTool;
	}

	public Float getMaximumTimeToCompleteJob() {
		return maximumTimeToCompleteJob != null ? maximumTimeToCompleteJob : DEFAULT_JOB_TIME;
	}

	public void setMaximumTimeToCompleteJob(Float maximumTimeToCompleteJob) {
		this.maximumTimeToCompleteJob = maximumTimeToCompleteJob;
	}

	public int getExperienceAwardedOnCompletion() {
		return experienceAwardedOnCompletion != null ? experienceAwardedOnCompletion : 1;
	}

	public void setExperienceAwardedOnCompletion(Integer experienceAwardedOnCompletion) {
		this.experienceAwardedOnCompletion = experienceAwardedOnCompletion;
	}


	public String getWorkOnJobAnimation() {
		return workOnJobAnimation;
	}

	public void setWorkOnJobAnimation(String workOnJobAnimation) {
		this.workOnJobAnimation = workOnJobAnimation;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (o == null || getClass() != o.getClass()) return false;

		JobType jobType = (JobType) o;

		return new EqualsBuilder().append(name, jobType.name).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(name).toHashCode();
	}

}
