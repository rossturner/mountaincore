package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.modding.LocalModRepository;
import technology.rocketjump.saul.modding.ModCompatibilityChecker;
import technology.rocketjump.saul.modding.model.ParsedMod;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MenuSkin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Singleton
public class ModsMenu implements Menu, DisplaysText {

	private final MenuSkin menuSkin;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final LocalModRepository modRepository;
	private final ModCompatibilityChecker modCompatibilityChecker;
	private final Stack stack = new Stack();

	private List<ParsedMod> modsInOrder = new ArrayList<>();

	@Inject
	public ModsMenu(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
	                I18nTranslator i18nTranslator, LocalModRepository modRepository,
	                ModCompatibilityChecker modCompatibilityChecker) {
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.modRepository = modRepository;
		this.modCompatibilityChecker = modCompatibilityChecker;
	}

	@Override
	public void show() {
		reset();
	}

	@Override
	public void hide() {

	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(stack);
	}

	@Override
	public void reset() {
		rebuildUI();
	}

	@Override
	public void rebuildUI() {
		modsInOrder.addAll(modRepository.getActiveMods());
		List<ParsedMod> inactiveMods = new ArrayList<>();
		for (ParsedMod mod : modRepository.getAll()) {
			if (!modsInOrder.contains(mod)) {
				inactiveMods.add(mod);
			}
		}
		inactiveMods.sort(Comparator.comparing(o -> o.getInfo().getName()));
		Collections.reverse(inactiveMods);
		modsInOrder.addAll(inactiveMods);
		Collections.reverse(modsInOrder);

		stack.clear();

		Label titleRibbon = new Label(i18nTranslator.translate("MENU.MODS"), menuSkin, "title_ribbon");


		Table mainTable = new Table();
		mainTable.center();
		mainTable.setBackground(menuSkin.getDrawable("asset_square_bg"));

		mainTable.add(titleRibbon).spaceTop(28f).spaceBottom(50f).row();

		stack.add(mainTable);


//		outerTable = new Table(uiSkin);
//		outerTable.setFillParent(false);
//		outerTable.center();
//		outerTable.background("default-rect");
////		outerTable.setDebug(true);
//
//		modsTable = new Table(uiSkin);
//

//		backButton = iconButtonFactory.create("GUI.BACK_LABEL", null, Color.LIGHT_GRAY, ButtonStyle.SMALL);
//		backButton.setAction(() -> {
//			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
//			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
//			if (modRepository.hasChangesToApply()) {
//				messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_INFO, InfoType.MOD_CHANGES_OUTSTANDING);
//			}
//		});


		//		outerTable.clearChildren();
//
//		modsTable.clearChildren();
//
//		modsTable.add(i18NWidgetFactory.createLabel("MODS.TABLE.ORDERING")).pad(10);
//		modsTable.add(i18NWidgetFactory.createLabel("MODS.TABLE.ENABLED")).pad(10);
//		modsTable.add(i18NWidgetFactory.createLabel("MODS.TABLE.NAME")).pad(10);
//		modsTable.add(i18NWidgetFactory.createLabel("MODS.TABLE.VERSION")).pad(10);
//		modsTable.add(i18NWidgetFactory.createLabel("MODS.TABLE.COMPATIBILITY")).pad(10);
//		modsTable.row();
//
//		for (int i = 0; i < modsInOrder.size(); i++) {
//			final int index = i;
//			ParsedMod mod = modsInOrder.get(index);
//			final boolean isBaseMod = mod.getInfo().isBaseMod();
//
//			if (isBaseMod) {
//				modsTable.add(new Container<>());
//			} else {
//				Table orderingTable = new Table(uiSkin);
//				IconOnlyButton upButton = iconButtonFactory.create("arrow-up");
//				upButton.setAction(() -> {
//					Collections.swap(modsInOrder, index, index - 1);
//					orderChanged();
//				});
//				IconOnlyButton downButton = iconButtonFactory.create("arrow-down");
//				downButton.setAction(() -> {
//					Collections.swap(modsInOrder, index, index + 1);
//					orderChanged();
//				});
//
//				if (index > 0) {
//					orderingTable.add(upButton).pad(5);
//				}
//
//				if (index < modsInOrder.size() - 2) {
//					orderingTable.add(downButton).pad(5);
//				}
//
//				modsTable.add(orderingTable);
//			}
//
//			ModCompatibilityChecker.Compatibility compatibility = modCompatibilityChecker.checkCompatibility(mod);
//
//			CheckBox activeCheckbox = new CheckBox("", uiSkin);
//			activeCheckbox.getLabelCell().padLeft(5f);
//			activeCheckbox.setChecked(modRepository.getActiveMods().contains(mod));
//			if (isBaseMod) {
//				activeCheckbox.setDisabled(true);
//			}
//			activeCheckbox.addListener((event) -> {
//				if (event instanceof ChangeListener.ChangeEvent) {
//					boolean checked = activeCheckbox.isChecked();
//					if (isBaseMod) {
//						return true;
//					} else if (checked) {
//						modRepository.getActiveMods().add(mod);
//						orderChanged();
//					} else {
//						modRepository.getActiveMods().remove(mod);
//						orderChanged();
//					}
//				}
//				return true;
//			});
//			if (compatibility.equals(INCOMPATIBLE)) {
//				// Disabled unchecked box is unclear so just adding an empty cell
//				modsTable.add(new Container<>());
//			} else {
//				modsTable.add(activeCheckbox);
//			}
//
//			modsTable.add(new Label(mod.getInfo().getName(), uiSkin));
//			modsTable.add(new Label(mod.getInfo().getVersion().toString(), uiSkin));
//
//			I18nLabel i18nLabel = i18NWidgetFactory.createLabel(compatibility.getI18nKey());
//			// New label instance so it can be reused in the same stage/table
//			modsTable.add(new Label(i18nLabel.getText(), uiSkin));
//			modsTable.row();
//		}
//
//		if (useScrollPane) {
//			outerTable.add(scrollPane).colspan(2).pad(10).left().row();
//		} else {
//			outerTable.add(modsTable).colspan(2).pad(10).left().row();
//		}
//
//
//		outerTable.add(backButton).colspan(2).pad(10).left();
//
//		outerTable.row();

	}

//	private void orderChanged() {
//		List<ParsedMod> currentActiveMods = modRepository.getActiveMods();
//		List<ParsedMod> newActiveMods = new ArrayList<>();
//
//		for (ParsedMod mod : modsInOrder) {
//			if (currentActiveMods.contains(mod)) {
//				newActiveMods.add(mod);
//			}
//		}
//
//		Collections.reverse(newActiveMods);
//		modRepository.setActiveMods(newActiveMods);
//
//		this.reset();
//	}


	//		BlurredBackgroundDialog dialog = new BlurredBackgroundDialog(I18nText.BLANK, skin, messageDispatcher, skin.get("square_dialog", Window.WindowStyle.class), soundAssetDictionary);
//		Label titleRibbon = new Label(i18nTranslator.translate("GUI.OPTIONS.KEY_BINDINGS"), skin, "title_ribbon");
//		Label gameplayLabel = new Label(i18nTranslator.translate(OptionsTabName.GAMEPLAY.getI18nKey()), skin, "secondary_banner_title");
//		gameplayLabel.setAlignment(Align.center);
//		KeyBindingUIWidget keyBindingUIWidget = new KeyBindingUIWidget(skin, userPreferences, i18nTranslator, messageDispatcher, soundAssetDictionary);
//		Container<TextButton> resetBindingsButton = menuButtonFactory.createButton("GUI.OPTIONS.RESET", skin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
//				.withAction(() -> keyBindingUIWidget.resetToDefaultSettings())
//				.build();
//
//		ScrollPane scrollPane = new EnhancedScrollPane(keyBindingUIWidget, skin);
//		scrollPane.setForceScroll(false, true);
//		scrollPane.setFadeScrollBars(false);
//		scrollPane.setScrollbarsVisible(true);
//		scrollPane.setScrollBarPositions(true, true);
//
//		dialog.getContentTable().defaults().padLeft(120f).padRight(120f);
//		dialog.getContentTable().add(titleRibbon).spaceTop(28f).spaceBottom(50f).row();
//		dialog.getContentTable().add(gameplayLabel).align(Align.left).row();
//		dialog.getContentTable().add(scrollPane).fillX().height(1256f).padBottom(50f).row();
//		dialog.getContentTable().add(resetBindingsButton).padBottom(100f).row();
//
//
//		messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);

}
