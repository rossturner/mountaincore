package technology.rocketjump.saul.jobs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import technology.rocketjump.saul.misc.Name;
import technology.rocketjump.saul.ui.widgets.ImageButton;

public class Skill {

	@Name
	private String name;
	private String i18nKey;
	private SkillType type;
	private String icon;
	private String draggableIcon;
	@JsonIgnore
	private ImageButton imageButton;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public ImageButton getImageButton() {
		return imageButton;
	}

	public void setImageButton(ImageButton imageButton) {
		this.imageButton = imageButton.clone();
	}

	public SkillType getType() {
		return type;
	}

	public void setType(SkillType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Skill that = (Skill) o;
		return new EqualsBuilder().append(name, that.name).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(name).toHashCode();
	}

	public String getDraggableIcon() {
		return draggableIcon;
	}

	public void setDraggableIcon(String draggableIcon) {
		this.draggableIcon = draggableIcon;
	}
}
