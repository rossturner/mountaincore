package technology.rocketjump.mountaincore.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.EntityStore;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.jobs.JobStore;
import technology.rocketjump.mountaincore.jobs.JobTypeDictionary;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.production.StockpileComponentUpdater;
import technology.rocketjump.mountaincore.production.StockpileGroupDictionary;
import technology.rocketjump.mountaincore.rendering.entities.EntityRenderer;
import technology.rocketjump.mountaincore.screens.SettlerManagementScreen;
import technology.rocketjump.mountaincore.ui.GameInteractionStateContainer;
import technology.rocketjump.mountaincore.ui.Selectable;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.widgets.ButtonFactory;
import technology.rocketjump.mountaincore.ui.widgets.crafting.CraftingHintWidgetFactory;
import technology.rocketjump.mountaincore.ui.widgets.furniture.ProductionExportFurnitureWidget;
import technology.rocketjump.mountaincore.ui.widgets.furniture.ProductionImportFurnitureWidget;
import technology.rocketjump.mountaincore.ui.widgets.text.DecoratedStringLabelFactory;

@Singleton
public class DoorwaySelectedGuiView extends EntitySelectedGuiView {

	@Inject
	public DoorwaySelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
								  I18nTranslator i18nTranslator, GameInteractionStateContainer gameInteractionStateContainer,
								  EntityStore entityStore, JobStore jobStore, JobTypeDictionary jobTypeDictionary,
								  TooltipFactory tooltipFactory, DecoratedStringLabelFactory decoratedStringLabelFactory,
								  EntityRenderer entityRenderer, ButtonFactory buttonFactory, StockpileComponentUpdater stockpileComponentUpdater,
								  StockpileGroupDictionary stockpileGroupDictionary, GameMaterialDictionary gameMaterialDictionary, RaceDictionary raceDictionary,
								  ItemTypeDictionary itemTypeDictionary, SoundAssetDictionary soundAssetDictionary,
								  SettlerManagementScreen settlerManagementScreen, CraftingHintWidgetFactory craftingHintWidgetFactory,
								  ProductionImportFurnitureWidget productionImportFurnitureWidget,
								  ProductionExportFurnitureWidget productionExportFurnitureWidget) {

		super(guiSkinRepository, messageDispatcher, i18nTranslator, gameInteractionStateContainer, entityStore, jobStore, jobTypeDictionary, tooltipFactory,
				productionImportFurnitureWidget, productionExportFurnitureWidget,
				decoratedStringLabelFactory, entityRenderer, buttonFactory, craftingHintWidgetFactory, stockpileComponentUpdater, stockpileGroupDictionary, gameMaterialDictionary,
				raceDictionary, itemTypeDictionary, soundAssetDictionary, settlerManagementScreen);
	}

	@Override
	protected Entity getSelectedEntity() {
		Selectable selectable = gameInteractionStateContainer.getSelectable();
		if (selectable != null && selectable.type.equals(Selectable.SelectableType.DOORWAY)) {
			return selectable.getDoorway().getDoorEntity();
		} else {
			return null;
		}
	}


	@Override
	public GuiViewName getName() {
		return GuiViewName.DOORWAY_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

}
