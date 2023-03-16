package technology.rocketjump.saul.production;

import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import technology.rocketjump.saul.misc.Name;
import technology.rocketjump.saul.rendering.utils.HexColors;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockpileGroup {

	@Name
	private String name;
	private String i18nKey;
	private String drawableName;
	private String overviewDrawableName;
	private String colorCode;
	@JsonIgnore
	private Color color = HexColors.POSITIVE_COLOR;
	private String iconName;
	private int sortOrder = 0;
	private boolean includesCreatureCorpses;

	public StockpileGroup() {

	}

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

	public String getColorCode() {
		return colorCode;
	}

	public void setColorCode(String colorCode) {
		this.colorCode = colorCode;
		if (colorCode != null) {
			this.color = HexColors.get(colorCode);
		}
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public String getIconName() {
		return iconName;
	}

	public void setIconName(String iconName) {
		this.iconName = iconName;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StockpileGroup that = (StockpileGroup) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	public boolean isIncludesCreatureCorpses() {
		return includesCreatureCorpses;
	}

	public void setIncludesCreatureCorpses(boolean includesCreatureCorpses) {
		this.includesCreatureCorpses = includesCreatureCorpses;
	}

	public String getDrawableName() {
		return drawableName;
	}

	public void setDrawableName(String drawableName) {
		this.drawableName = drawableName;
	}

	public String getOverviewDrawableName() {
		return overviewDrawableName;
	}

	public void setOverviewDrawableName(String overviewDrawableName) {
		this.overviewDrawableName = overviewDrawableName;
	}
}
