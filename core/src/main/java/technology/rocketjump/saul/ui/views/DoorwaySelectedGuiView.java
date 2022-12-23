package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.EntityStore;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.production.StockpileComponentUpdater;
import technology.rocketjump.saul.production.StockpileGroupDictionary;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.screens.SettlerManagementScreen;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.ButtonFactory;
import technology.rocketjump.saul.ui.widgets.text.DecoratedStringLabelFactory;

import static technology.rocketjump.saul.ui.Selectable.SelectableType.DOORWAY;

@Singleton
public class DoorwaySelectedGuiView extends EntitySelectedGuiView {

	@Inject
	public DoorwaySelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
	                              I18nTranslator i18nTranslator, GameInteractionStateContainer gameInteractionStateContainer,
	                              EntityStore entityStore, JobStore jobStore, JobTypeDictionary jobTypeDictionary,
	                              TooltipFactory tooltipFactory, DecoratedStringLabelFactory decoratedStringLabelFactory,
	                              EntityRenderer entityRenderer, ButtonFactory buttonFactory, StockpileComponentUpdater stockpileComponentUpdater,
	                              StockpileGroupDictionary stockpileGroupDictionary, GameMaterialDictionary gameMaterialDictionary, RaceDictionary raceDictionary,
	                              ItemTypeDictionary itemTypeDictionary, SoundAssetDictionary soundAssetDictionary, SettlerManagementScreen settlerManagementScreen) {

		super(guiSkinRepository, messageDispatcher, i18nTranslator, gameInteractionStateContainer, entityStore, jobStore, jobTypeDictionary, tooltipFactory,
				decoratedStringLabelFactory, entityRenderer, buttonFactory, stockpileComponentUpdater, stockpileGroupDictionary, gameMaterialDictionary,
				raceDictionary, itemTypeDictionary, soundAssetDictionary, settlerManagementScreen);
	}

	@Override
	protected Entity getSelectedEntity() {
		Selectable selectable = gameInteractionStateContainer.getSelectable();
		if (selectable != null && selectable.type.equals(DOORWAY)) {
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
