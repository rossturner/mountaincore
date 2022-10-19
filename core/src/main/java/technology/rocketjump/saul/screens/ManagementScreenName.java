package technology.rocketjump.saul.screens;

import java.util.List;

public enum ManagementScreenName {

	SETTLERS("GUI.SETTLER_MANAGEMENT.TITLE", "btn_top_settlers"),
	RESOURCES("GUI.RESOURCE_MANAGEMENT.TITLE", "btn_top_resources"),
	CRAFTING("GUI.CRAFTING_MANAGEMENT.TITLE", "btn_top_crafting");

	public final String titleI18nKey;
	public final String buttonStyleName;

	ManagementScreenName(String titleI18nKey, String buttonStyleName) {
		this.titleI18nKey = titleI18nKey;
		this.buttonStyleName = buttonStyleName;
	}

	public static List<ManagementScreenName> managementScreensOrderedForUI = List.of(CRAFTING, RESOURCES, SETTLERS);

}
