package technology.rocketjump.mountaincore.cooking.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;

public class CookingSession implements ChildPersistable {

	private CookingRecipe recipe;
	private Entity assignedFurnitureEntity;
	private double gameTimeStart;

	private List<Job> inputIngredientJobs = new ArrayList<>();
	private Job cookingJob = null;
	private boolean completed = false;

	public CookingSession() {

	}

	public CookingSession(CookingRecipe recipe, Entity assignedFurnitureEntity, double gameTimeStart) {
		this.recipe = recipe;
		this.assignedFurnitureEntity = assignedFurnitureEntity;
		this.gameTimeStart = gameTimeStart;
	}

	public CookingRecipe getRecipe() {
		return recipe;
	}

	public Entity getAssignedFurnitureEntity() {
		return assignedFurnitureEntity;
	}

	public double getGameTimeStart() {
		return gameTimeStart;
	}

	public List<Job> getInputIngredientJobs() {
		return inputIngredientJobs;
	}

	public Job getCookingJob() {
		return cookingJob;
	}

	public void setCookingJob(Job cookingJob) {
		this.cookingJob = cookingJob;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("recipe", recipe.getRecipeName());
		asJson.put("entity", assignedFurnitureEntity.getId());
		asJson.put("gameTimeStart", gameTimeStart);

		if (!inputIngredientJobs.isEmpty()) {
			JSONArray inputJobs = new JSONArray();
			for (Job inputIngredientJob : inputIngredientJobs) {
				inputIngredientJob.writeTo(savedGameStateHolder);
				inputJobs.add(inputIngredientJob.getJobId());
			}
			asJson.put("inputs", inputJobs);
		}
		if (cookingJob != null) {
			cookingJob.writeTo(savedGameStateHolder);
			asJson.put("cookingJob", cookingJob.getJobId());
		}
		if (completed) {
			asJson.put("completed", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.recipe = relatedStores.cookingRecipeDictionary.getByName(asJson.getString("recipe"));
		if (this.recipe == null) {
			throw new InvalidSaveException("Could not find recipe by name " + asJson.getString("recipe"));
		}
		this.assignedFurnitureEntity = savedGameStateHolder.entities.get(asJson.getLongValue("entity"));
		if (this.assignedFurnitureEntity == null) {
			throw new InvalidSaveException("Could not find entity by ID " + asJson.getLongValue("entity"));
		}
		JSONArray inputsJson = asJson.getJSONArray("inputs");
		if (inputsJson != null) {
			for (int cursor = 0; cursor < inputsJson.size(); cursor++) {
				Job inputJob = savedGameStateHolder.jobs.get(inputsJson.getLongValue(cursor));
				if (inputJob == null) {
					throw new InvalidSaveException("Could not find hauling allocation by ID " + inputsJson.getLongValue(cursor));
				} else {
					inputIngredientJobs.add(inputJob);
				}
			}
		}
		Long cookingJobId = asJson.getLong("cookingJob");
		if (cookingJobId != null) {
			this.cookingJob = savedGameStateHolder.jobs.get(cookingJobId);
			if (this.cookingJob == null) {
				throw new InvalidSaveException("Could not find job by ID " + cookingJobId);
			}
		}
		this.completed = asJson.getBooleanValue("completed");
		this.gameTimeStart = asJson.getDoubleValue("gameTimeStart");
	}
}
