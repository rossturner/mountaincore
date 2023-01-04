package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rooms.Bridge;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MainGameSkin;
import technology.rocketjump.saul.ui.skins.ManagementSkin;
import technology.rocketjump.saul.ui.widgets.ButtonFactory;

@Singleton
public class BridgeSelectedGuiView implements GuiView, GameContextAware, DisplaysText {

	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final MessageDispatcher messageDispatcher;
	private final MainGameSkin mainGameSkin;
	private final ManagementSkin managementSkin;
	private final ButtonFactory buttonFactory;
	private final TooltipFactory tooltipFactory;

	private Selectable currentSelectable;
	private Table outerTable;

	@Inject
	public BridgeSelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
	                             GameInteractionStateContainer gameInteractionStateContainer, ButtonFactory buttonFactory, TooltipFactory tooltipFactory) {
		this.mainGameSkin = guiSkinRepository.getMainGameSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.messageDispatcher = messageDispatcher;
		this.buttonFactory = buttonFactory;
		this.tooltipFactory = tooltipFactory;

		this.outerTable = new Table();
	}

	@Override
	public void populate(Table containerTable) {
		update();

		containerTable.clear();
		containerTable.add(outerTable);
	}

	@Override
	public void update() {
		outerTable.clear();

		outerTable.setTouchable(Touchable.enabled);
		outerTable.setBackground(mainGameSkin.getDrawable("ENTITY_SELECT_BG_SMALL"));

		Selectable selectable = gameInteractionStateContainer.getSelectable();
		if (selectable != null && selectable.getBridge() != null) {
			Bridge bridge = selectable.getBridge();

			Container<Label> headerContainer = new Container<>(new Label(i18nTranslator.getDescription(bridge).toString(), mainGameSkin.get("title-header", Label.LabelStyle.class)));
			headerContainer.setBackground(mainGameSkin.get("asset_bg_ribbon_title_patch", TenPatchDrawable.class));

			Table actionButtons = actionButtons(bridge);

			String deconstructionLabelText = i18nTranslator.translate("GUI.FURNITURE_BEING_REMOVED");

			outerTable.clear();

			actionButtons.layout();
			outerTable.add(new Container<>()).left().width(actionButtons.getPrefWidth()).padTop(67).padLeft(67);
			outerTable.add(headerContainer).fillX().padTop(67);
			outerTable.add(actionButtons).right().padTop(67).padRight(67);
			outerTable.row();

			Table viewContents = new Table();
			viewContents.defaults().growY().spaceBottom(20);
			if (bridge.isBeingDeconstructed()) {
				viewContents.add(new Label(deconstructionLabelText, managementSkin, "default-font-18-label")).grow().row();
			}

			outerTable.add(viewContents).colspan(3).growY().padRight(67).padLeft(67).padTop(20).row();

		}
	}

	private Table actionButtons(Bridge bridge) {
		Table table = new Table();
		table.defaults().pad(18);
		Container<Button> deconstructContainer = new Container<>();
		if (bridge.isBeingDeconstructed()) {
			deconstructContainer.setBackground(mainGameSkin.getDrawable("asset_selection_bg_cropped"));
		}

		Button deconstructButton = new Button(mainGameSkin.getDrawable("btn_demolish_small"));
		deconstructButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				if (!bridge.isBeingDeconstructed()) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_BRIDGE_REMOVAL, bridge);
					update();
				}
			}
		});

		if (bridge.isBeingDeconstructed()) {
			buttonFactory.disable(deconstructButton);
		}
		buttonFactory.attachClickCursor(deconstructButton, GameCursor.SELECT);
		tooltipFactory.simpleTooltip(deconstructButton, "GUI.DECONSTRUCT_LABEL", TooltipLocationHint.ABOVE);
		deconstructContainer.setActor(deconstructButton);
		table.add(deconstructContainer);
		return table;
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.BRIDGE_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

	@Override
	public void onContextChange(GameContext gameContext) {

	}

	@Override
	public void clearContextRelatedState() {
		currentSelectable = null;
	}

	@Override
	public void rebuildUI() {
		update();
	}
}
