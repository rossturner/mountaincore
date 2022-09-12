package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.rooms.constructions.Construction;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.ButtonStyle;
import technology.rocketjump.saul.ui.widgets.I18nTextWidget;
import technology.rocketjump.saul.ui.widgets.IconButton;
import technology.rocketjump.saul.ui.widgets.IconButtonFactory;

import java.util.List;

import static technology.rocketjump.saul.ui.Selectable.SelectableType.CONSTRUCTION;

@Singleton
public class ConstructionSelectedGuiView implements GuiView, GameContextAware {

	private final Skin uiSkin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final IconButton cancelButton;
	private final MessageDispatcher messageDispatcher;
	private Table outerTable;
	private Table descriptionTable;
	private GameContext gameContext;

	@Inject
	public ConstructionSelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
									   GameInteractionStateContainer gameInteractionStateContainer, IconButtonFactory iconButtonFactory) {
		uiSkin = guiSkinRepository.getDefault();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;

		outerTable = new Table(uiSkin);
		outerTable.background("default-rect");
		outerTable.pad(10);

		descriptionTable = new Table(uiSkin);
		descriptionTable.pad(10);

		cancelButton = iconButtonFactory.create("GUI.CANCEL_LABEL", "cancel", HexColors.get("#D4534C"), ButtonStyle.SMALL);
		cancelButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (selectable != null && selectable.type.equals(CONSTRUCTION)) {
				messageDispatcher.dispatchMessage(MessageType.CANCEL_CONSTRUCTION, selectable.getConstruction());
			}
		});

		outerTable.add(descriptionTable).left();
		outerTable.add(cancelButton).center();
	}

	@Override
	public void populate(Table containerTable) {
		update();

		containerTable.clear();
		containerTable.add(outerTable);
	}

	@Override
	public void update() {
		descriptionTable.clear();

		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if (selectable != null && selectable.type.equals(CONSTRUCTION)) {
			Construction construction = selectable.getConstruction();
			List<I18nText> description = construction.getDescription(i18nTranslator, gameContext);
			for (I18nText line : description) {
				descriptionTable.add(new I18nTextWidget(line, uiSkin, messageDispatcher)).left();
				descriptionTable.row();
			}
		}
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.CONSTRUCTION_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
