package technology.rocketjump.mountaincore.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.StockpileSettingsUpdatedMessage;
import technology.rocketjump.mountaincore.production.StockpileComponentUpdater;
import technology.rocketjump.mountaincore.production.StockpileGroup;
import technology.rocketjump.mountaincore.production.StockpileGroupDictionary;
import technology.rocketjump.mountaincore.production.StockpileSettings;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.components.StockpileRoomComponent;
import technology.rocketjump.mountaincore.ui.Scene2DUtils;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.mountaincore.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.util.List;

public class StockpileManagementTree extends Table {

	private final StockpileComponentUpdater stockpileComponentUpdater;
	private final ScrollPane scrollPane;
	private final Skin uiSkin;
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;
	private final long haulingTargetId;
	private final HaulingAllocation.AllocationPositionType haulingTargetPositionType;
	private final StockpileSettings stockpileSettings;
	private final SoundAssetDictionary soundAssetDictionary;
	private final StockpileRoomComponent stockpileRoomComponent;

	public StockpileManagementTree(Skin uiSkin, MessageDispatcher messageDispatcher,
								   StockpileComponentUpdater stockpileComponentUpdater, StockpileGroupDictionary stockpileGroupDictionary,
								   I18nTranslator i18nTranslator, ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary gameMaterialDictionary,
								   RaceDictionary raceDictionary, Race settlerRace, long haulingTargetId, HaulingAllocation.AllocationPositionType haulingTargetPositionType,
								   StockpileSettings stockpileSettings, SoundAssetDictionary soundAssetDictionary, StockpileRoomComponent stockpileRoomComponent) {
		this.uiSkin = uiSkin;
		this.stockpileComponentUpdater = stockpileComponentUpdater;
		this.i18nTranslator = i18nTranslator;
		this.messageDispatcher = messageDispatcher;
		this.stockpileSettings = stockpileSettings;
		this.haulingTargetPositionType = haulingTargetPositionType;
		this.haulingTargetId = haulingTargetId;
		this.soundAssetDictionary = soundAssetDictionary;
		this.stockpileRoomComponent = stockpileRoomComponent;

		Tree<StockpileTreeNode, String> treeRoot = new Tree<>(uiSkin);

		for (StockpileGroup stockpileGroup : stockpileGroupDictionary.getAll(i18nTranslator)) {
			if (!stockpileSettings.isAllowed(stockpileGroup)) {
				continue;
			}

			StockpileTreeNode groupNode = new StockpileTreeNode();
			groupNode.setValue(new StockpileTreeValue(stockpileGroup));
			createCheckbox(groupNode, stockpileGroup.getI18nKey());
			treeRoot.add(groupNode);

			boolean allItemsDisabled = true;
			boolean allItemsEnabled = true;

			if (stockpileGroup.isIncludesCreatureCorpses()) {
				StockpileTreeNode corpseGroupNode = new StockpileTreeNode();
				corpseGroupNode.setValue(new StockpileTreeValue("Corpse group"));
				createCheckbox(corpseGroupNode, "CARCASS.GROUP");
				groupNode.add(corpseGroupNode);

				if (this.stockpileSettings.isAcceptingCorpses()) {
					allItemsDisabled = false;
				} else {
					allItemsEnabled = false;
				}

				boolean allRacesEnabled = true;
				boolean allRacesDisabled = true;

				for (Race race : raceDictionary.getAll()) {
					if (race.equals(settlerRace)) {
						continue;
					}
					StockpileTreeNode raceNode = new StockpileTreeNode();
					raceNode.setValue(new StockpileTreeValue(race));
					createCheckbox(raceNode, race.getI18nKey());
					corpseGroupNode.add(raceNode);

					if (this.stockpileSettings.isEnabled(race)) {
						allRacesDisabled = false;
					} else {
						allRacesEnabled = false;
					}
				}

				if (!allRacesDisabled && !allRacesEnabled) {
					groupNode.setExpanded(true);
					// setting these to cascade expansion up to parent
					allItemsEnabled = false;
					allItemsDisabled = false;
				}

			}

			for (ItemType itemType : itemTypeDictionary.getByStockpileGroup(stockpileGroup)) {
				if (!stockpileSettings.isAllowed(itemType)) {
					continue;
				}

				StockpileTreeNode itemTypeNode = new StockpileTreeNode();
				itemTypeNode.setValue(new StockpileTreeValue(itemType));
				createCheckbox(itemTypeNode, itemType.getI18nKey());
				groupNode.add(itemTypeNode);

				if (this.stockpileSettings.isEnabled(itemType)) {
					allItemsDisabled = false;
				} else {
					allItemsEnabled = false;
				}

				boolean allMaterialsEnabled = true;
				boolean allMaterialsDisabled = true;

				List<GameMaterial> materialsForItem = itemType.getSpecificMaterials();
				if (materialsForItem.isEmpty()) {
					materialsForItem = gameMaterialDictionary.getByType(itemType.getPrimaryMaterialType());
				}
				for (GameMaterial material : materialsForItem) {
					if (material.isUseMaterialTypeAsAdjective()) {
						// This is for wood types from bushes that should not show up anywhere, so skip them here
						continue;
					}
					StockpileTreeNode materialNode = new StockpileTreeNode();
					materialNode.setValue(new StockpileTreeValue(material, itemType));
					createCheckbox(materialNode, material.getI18nKey());
					itemTypeNode.add(materialNode);


					if (this.stockpileSettings.isEnabled(material, itemType)) {
						allMaterialsDisabled = false;
					} else {
						allMaterialsEnabled = false;
					}
				}

				if (!allMaterialsEnabled && !allMaterialsDisabled) {
					itemTypeNode.setExpanded(true);
					// setting these to cascade expansion up to parent
					allItemsEnabled = false;
					allItemsDisabled = false;
				}
			}

			if (!allItemsEnabled && !allItemsDisabled) {
				groupNode.setExpanded(true);
			}
		}

		scrollPane = Scene2DUtils.wrapWithScrollPane(treeRoot, uiSkin);
		this.add(scrollPane).width(350).height(400).left();
	}

	@Override
	public void setSize(float width, float height) {
		super.setSize(width, height);
		this.clearChildren();
		this.add(scrollPane).width(width).height(height).left();
	}

	private void createCheckbox(StockpileTreeNode node, String i18nKey) {
		CheckBox checkbox = new CheckBox(i18nTranslator.getTranslatedString(i18nKey).toString(), uiSkin);
		checkbox.getLabelCell().padLeft(5f);
		checkbox.setProgrammaticChangeEvents(false);
		checkbox.addListener(new StockpileTreeNodeEventListener(node));
		checkbox.addListener(new ChangeCursorOnHover(checkbox, GameCursor.SELECT, messageDispatcher));
		checkbox.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary, "VeryLightHover", "ConfirmVeryLight"));
		node.setActor(checkbox);
		updateCheckedState(node);
	}

	private void updateCheckedState(StockpileTreeNode node) {
		if (node.getValue().isGroup()) {
			node.getActor().setChecked(stockpileSettings.isEnabled(node.getValue().group));
		} else if (node.getValue().isItemType()) {
			node.getActor().setChecked(stockpileSettings.isEnabled(node.getValue().itemType));
		} else if (node.getValue().isMaterial()) {
			node.getActor().setChecked(stockpileSettings.isEnabled(node.getValue().material, node.getValue().itemType));
		} else if (node.getValue().isCorpseGroup) {
			node.getActor().setChecked(stockpileSettings.isAcceptingCorpses());
		} else if (node.getValue().isRace()) {
			node.getActor().setChecked(stockpileSettings.isEnabled(node.getValue().race));
		}
	}

	private void updateChildren(StockpileTreeNode node) {
		Array<StockpileTreeNode> children = node.getChildren();
		if (children != null && !children.isEmpty()) {
			for (StockpileTreeNode childNode : children) {
				updateCheckedState(childNode);
				updateChildren(childNode);
			}
		}
	}

	private class StockpileTreeNodeEventListener implements EventListener {


		private final StockpileTreeNode node;

		public StockpileTreeNodeEventListener(StockpileTreeNode node) {
			this.node = node;
		}

		@Override
		public boolean handle(Event event) {
			if (event instanceof ChangeListener.ChangeEvent) {
				StockpileTreeValue value = node.getValue();

				if (value.isGroup()) {
					stockpileComponentUpdater.toggleGroup(stockpileSettings, value.group, node.getActor().isChecked(), true);
				} else if (value.isItemType()) {
					stockpileComponentUpdater.toggleItem(stockpileSettings, value.itemType, node.getActor().isChecked(), true, true);
				} else if (value.isMaterial()) {
					stockpileComponentUpdater.toggleMaterial(stockpileSettings, value.itemType, value.material, node.getActor().isChecked(), true);
				} else if (value.isCorpseGroup) {
					stockpileComponentUpdater.toggleCorpseGroup(stockpileSettings, node.getActor().isChecked(), node.getParent().getValue().group, true, true);
				} else if (value.isRace()) {
					stockpileComponentUpdater.toggleRaceCorpse(stockpileSettings, value.race, node.getParent().getParent().getValue().group, node.getActor().isChecked(), true);
				}
				messageDispatcher.dispatchMessage(MessageType.STOCKPILE_SETTING_UPDATED, new StockpileSettingsUpdatedMessage(stockpileSettings, haulingTargetId, haulingTargetPositionType));

				StockpileTreeNode parent = node.getParent();
				while (parent != null) {
					updateCheckedState(parent);
					parent = parent.getParent();
				}

				updateChildren(node);

				Array<StockpileTreeNode> children = node.getChildren();
				if (children != null && children.isEmpty()) {
					for (StockpileTreeNode childNode : children) {
						updateCheckedState(childNode);
						updateChildren(childNode);
					}
				}
				if (stockpileRoomComponent != null) {
					stockpileRoomComponent.updateColor();
				}

				return true;
			} else {
				return false;
			}
		}

	}

	public static class StockpileTreeValue {

		public final StockpileGroup group;
		public final ItemType itemType;
		public final GameMaterial material;
		public final boolean isCorpseGroup;
		public final Race race;

		public StockpileTreeValue(StockpileGroup group) {
			this.group = group;
			this.itemType = null;
			this.material = null;
			this.isCorpseGroup = false;
			this.race = null;
		}

		public StockpileTreeValue(ItemType itemType) {
			this.group = null;
			this.itemType = itemType;
			this.material = null;
			this.isCorpseGroup = false;
			this.race = null;
		}

		public StockpileTreeValue(GameMaterial material, ItemType itemType) {
			this.group = null;
			this.itemType = itemType;
			this.material = material;
			this.isCorpseGroup = false;
			this.race = null;
		}

		public StockpileTreeValue(String unused) {
			this.group = null;
			this.itemType = null;
			this.material = null;
			this.isCorpseGroup = true;
			this.race = null;
		}

		public StockpileTreeValue(Race race) {
			this.group = null;
			this.itemType = null;
			this.material = null;
			this.isCorpseGroup = false;
			this.race = race;
		}

		public boolean isGroup() {
			return group != null;
		}

		public boolean isItemType() {
			return itemType != null && material == null;
		}

		public boolean isMaterial() {
			return material != null;
		}

		public boolean isRace() {
			return race != null;
		}
	}

	public static class StockpileTreeNode extends Tree.Node<StockpileTreeNode, StockpileTreeValue, CheckBox> {
		
	}
}
