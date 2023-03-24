package technology.rocketjump.mountaincore.entities.model.physical.furniture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.misc.Name;
import technology.rocketjump.mountaincore.rooms.RoomType;
import technology.rocketjump.mountaincore.ui.views.GuiViewName;

import java.util.*;

public class FurnitureType {

	@Name
	private String name;
	private String i18nKey;
	private boolean blocksMovement;

	private String defaultLayoutName;
	@JsonIgnore
	private FurnitureLayout defaultLayout;

	private GuiViewName showInGuiView; // Not restricted by room type
	@JsonIgnore
	private final Set<RoomType> validRoomTypes = new HashSet<>(); // Only set by code on load

	private boolean autoConstructed; // Is automatically built as soon as all requirements are in place

	// This is the list of items (with quantities) needed to build the type for each listed GameMaterialType
	private Map<GameMaterialType, List<QuantifiedItemType>> requirements;

	private Map<String, List<String>> tags = new HashMap<>();
	@JsonIgnore
	private List<Tag> processedTags = new ArrayList<>();

	private GameMaterialType requiredFloorMaterialType;

	private boolean hiddenFromPlacementMenu = false; // For furniture types that can't be placed by the player

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

	public String getDefaultLayoutName() {
		return defaultLayoutName;
	}

	public void setDefaultLayoutName(String defaultLayoutName) {
		this.defaultLayoutName = defaultLayoutName;
	}

	public FurnitureLayout getDefaultLayout() {
		return defaultLayout;
	}

	public void setDefaultLayout(FurnitureLayout defaultLayout) {
		this.defaultLayout = defaultLayout;
	}

	public boolean isAutoConstructed() {
		return autoConstructed;
	}

	public void setAutoConstructed(boolean autoConstructed) {
		this.autoConstructed = autoConstructed;
	}

	public Map<GameMaterialType, List<QuantifiedItemType>> getRequirements() {
		return requirements;
	}

	public void setRequirements(Map<GameMaterialType, List<QuantifiedItemType>> requirements) {
		this.requirements = requirements;
	}

	@Override
	public String toString() {
		return name;
	}

	@JsonIgnore
	public boolean isPlaceAnywhere() {
		return showInGuiView != null;
	}

	public Set<RoomType> getValidRoomTypes() {
		return validRoomTypes;
	}

	public Map<String, List<String>> getTags() {
		return tags;
	}

	public void setTags(Map<String, List<String>> tags) {
		this.tags = tags;
	}

	public void setProcessedTags(List<Tag> processedTags) {
		this.processedTags = processedTags;
	}

	public List<Tag> getProcessedTags() {
		return processedTags;
	}

	public boolean hasTag(Class<? extends Tag> tagClass) {
		return processedTags.stream().anyMatch(t -> t.getClass().equals(tagClass));
	}

	public GameMaterialType getRequiredFloorMaterialType() {
		return requiredFloorMaterialType;
	}

	public void setRequiredFloorMaterialType(GameMaterialType requiredFloorMaterialType) {
		this.requiredFloorMaterialType = requiredFloorMaterialType;
	}

	public boolean isHiddenFromPlacementMenu() {
		return hiddenFromPlacementMenu;
	}

	public void setHiddenFromPlacementMenu(boolean hiddenFromPlacementMenu) {
		this.hiddenFromPlacementMenu = hiddenFromPlacementMenu;
	}

	public boolean isBlocksMovement() {
		return blocksMovement;
	}

	public void setBlocksMovement(boolean blocksMovement) {
		this.blocksMovement = blocksMovement;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FurnitureType that = (FurnitureType) o;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public GuiViewName getShowInGuiView() {
		return showInGuiView;
	}

	public void setShowInGuiView(GuiViewName showInGuiView) {
		if (GuiViewName.NULL.equals(showInGuiView)) {
			this.showInGuiView = null;
		} else {
			this.showInGuiView = showInGuiView;
		}
	}
}
