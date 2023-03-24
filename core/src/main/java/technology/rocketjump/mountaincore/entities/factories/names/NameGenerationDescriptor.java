package technology.rocketjump.mountaincore.entities.factories.names;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.mountaincore.misc.Name;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NameGenerationDescriptor {

	@Name
	private String descriptorName;
	private String firstName;
	private String familyName;
	private List<String> goodSpheres;
	private List<String> badSpheres;

	public String getDescriptorName() {
		return descriptorName;
	}

	public void setDescriptorName(String descriptorName) {
		this.descriptorName = descriptorName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public List<String> getGoodSpheres() {
		return goodSpheres;
	}

	public void setGoodSpheres(List<String> goodSpheres) {
		this.goodSpheres = goodSpheres;
	}

	public List<String> getBadSpheres() {
		return badSpheres;
	}

	public void setBadSpheres(List<String> badSpheres) {
		this.badSpheres = badSpheres;
	}
}
