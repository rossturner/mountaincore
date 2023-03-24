package technology.rocketjump.mountaincore.settlement.production;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.crafting.model.CraftingRecipe;
import technology.rocketjump.mountaincore.entities.SequentialIdGenerator;
import technology.rocketjump.mountaincore.entities.components.LiquidAllocation;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.persistence.JSONUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.Persistable;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

import java.util.ArrayList;
import java.util.List;

public class CraftingAssignment implements Persistable {

	private long craftingAssignmentId;
	private CraftingRecipe targetRecipe;
	private Job craftingJob;
	private final List<HaulingAllocation> inputAllocations = new ArrayList<>();
	private final List<LiquidAllocation> inputLiquidAllocations = new ArrayList<>();
	private GridPoint2 outputLocation;

	public CraftingAssignment() {

	}

	public CraftingAssignment(CraftingRecipe targetRecipe) {
		this.craftingAssignmentId = SequentialIdGenerator.nextId();
		this.targetRecipe = targetRecipe;
	}

	public long getCraftingAssignmentId() {
		return craftingAssignmentId;
	}

	public Job getCraftingJob() {
		return craftingJob;
	}

	public void setCraftingJob(Job craftingJob) {
		this.craftingJob = craftingJob;
	}

	public CraftingRecipe getTargetRecipe() {
		return targetRecipe;
	}

	public List<HaulingAllocation> getInputAllocations() {
		return inputAllocations;
	}

	public List<LiquidAllocation> getInputLiquidAllocations() {
		return inputLiquidAllocations;
	}

	public GridPoint2 getOutputLocation() {
		return outputLocation;
	}

	public void setOutputLocation(GridPoint2 outputLocation) {
		this.outputLocation = outputLocation;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.craftingAssignments.containsKey(this.craftingAssignmentId)) {
			return;
		}

		JSONObject asJson = new JSONObject(true);
		asJson.put("craftingAssignmentId", craftingAssignmentId);
		asJson.put("targetRecipe", targetRecipe.getRecipeName());
		craftingJob.writeTo(savedGameStateHolder);
		asJson.put("craftingJob", craftingJob.getJobId());

		JSONArray inputAllocationsJson = new JSONArray();
		for (HaulingAllocation inputAllocation : inputAllocations) {
			inputAllocation.writeTo(savedGameStateHolder);
			inputAllocationsJson.add(inputAllocation.getHaulingAllocationId());
		}
		asJson.put("inputAllocations", inputAllocationsJson);

		if (!inputLiquidAllocations.isEmpty()) {
			JSONArray inputLiquidAllocationsJson = new JSONArray();
			for (LiquidAllocation liquidAllocation : inputLiquidAllocations) {
				liquidAllocation.writeTo(savedGameStateHolder);
				inputLiquidAllocationsJson.add(liquidAllocation.getLiquidAllocationId());
			}
			asJson.put("inputLiquidAllocations", inputLiquidAllocationsJson);
		}

		asJson.put("outputLocation", JSONUtils.toJSON(outputLocation));

		savedGameStateHolder.craftingAssignments.put(this.craftingAssignmentId, this);
		savedGameStateHolder.craftingAssignmentsJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.craftingAssignmentId = asJson.getLong("craftingAssignmentId");
		this.targetRecipe = relatedStores.craftingRecipeDictionary.getByName(asJson.getString("targetRecipe"));
		if (this.targetRecipe == null) {
			throw new InvalidSaveException("Could not find crafting recipe " + asJson.getString("targetRecipe"));
		}
		this.craftingJob = savedGameStateHolder.jobs.get(asJson.getLong("craftingJob"));
		if (this.craftingJob == null) {
			throw new InvalidSaveException("Could not find crafting job " + asJson.getLong("craftingJob"));
		}

		JSONArray inputAllocationsJson = asJson.getJSONArray("inputAllocations");
		for (int i = 0; i < inputAllocationsJson.size(); i++) {
			HaulingAllocation inputAllocation = savedGameStateHolder.haulingAllocations.get(inputAllocationsJson.getLong(i));
			if (inputAllocation == null) {
				throw new InvalidSaveException("Could not find input allocation " + inputAllocationsJson.getLong(i));
			}
			inputAllocations.add(inputAllocation);
		}

		JSONArray inputLiquidAllocationsJson = asJson.getJSONArray("inputLiquidAllocations");
		if (inputLiquidAllocationsJson != null) {
			for (int i = 0; i < inputLiquidAllocationsJson.size(); i++) {
				LiquidAllocation inputAllocation = savedGameStateHolder.liquidAllocations.get(inputLiquidAllocationsJson.getLong(i));
				if (inputAllocation == null) {
					throw new InvalidSaveException("Could not find input liquid allocation " + inputLiquidAllocationsJson.getLong(i));
				}
				inputLiquidAllocations.add(inputAllocation);
			}
		}

		this.outputLocation = JSONUtils.gridPoint2(asJson.getJSONObject("outputLocation"));

		savedGameStateHolder.craftingAssignments.put(this.craftingAssignmentId, this);
	}
}
