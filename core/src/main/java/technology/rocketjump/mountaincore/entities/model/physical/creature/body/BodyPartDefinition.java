package technology.rocketjump.mountaincore.entities.model.physical.creature.body;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.mountaincore.misc.Name;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BodyPartDefinition {

	@Name
	private String name;
	private float size;
	private List<BoneType> bones = new ArrayList<>();
	private List<BodyPartOrgan> organs = new ArrayList<>();
	private List<String> childParts = new ArrayList<>();
	private BodyPartFunction function;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getSize() {
		return size;
	}

	public void setSize(float size) {
		this.size = size;
	}

	public List<BoneType> getBones() {
		return bones;
	}

	public void setBones(List<BoneType> bones) {
		this.bones = bones;
	}

	public List<BodyPartOrgan> getOrgans() {
		return organs;
	}

	public void setOrgans(List<BodyPartOrgan> organs) {
		this.organs = organs;
	}

	public List<String> getChildParts() {
		return childParts;
	}

	public void setChildParts(List<String> childParts) {
		this.childParts = childParts;
	}

	public BodyPartFunction getFunction() {
		return function;
	}

	public void setFunction(BodyPartFunction function) {
		this.function = function;
	}

	@Override
	public String toString() {
		return name;
	}

	public String getI18nKey() {
		return "BODY_STRUCTURE."+name.toUpperCase(Locale.ROOT);
	}
}
