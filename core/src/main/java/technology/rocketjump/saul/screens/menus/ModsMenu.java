package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.codedisaster.steamworks.SteamAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.messaging.InfoType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.modding.CompatibilityResult;
import technology.rocketjump.saul.modding.LocalModRepository;
import technology.rocketjump.saul.modding.ModCompatibilityChecker;
import technology.rocketjump.saul.modding.authentication.ModioAuthManager;
import technology.rocketjump.saul.modding.authentication.ModioTermsConditions;
import technology.rocketjump.saul.modding.authentication.SteamUserManager;
import technology.rocketjump.saul.modding.authentication.TermsConditionsContent;
import technology.rocketjump.saul.modding.model.ParsedMod;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MainGameSkin;
import technology.rocketjump.saul.ui.skins.ManagementSkin;
import technology.rocketjump.saul.ui.skins.MenuSkin;
import technology.rocketjump.saul.ui.widgets.BlurredBackgroundDialog;
import technology.rocketjump.saul.ui.widgets.EnhancedScrollPane;
import technology.rocketjump.saul.ui.widgets.MenuButtonFactory;

import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.modding.authentication.TermsConditionsContent.ModioTCsButton.agree;
import static technology.rocketjump.saul.modding.authentication.TermsConditionsContent.ModioTCsButton.disagree;
import static technology.rocketjump.saul.modding.authentication.TermsConditionsContent.ModioTCsLinkName.privacy;
import static technology.rocketjump.saul.modding.authentication.TermsConditionsContent.ModioTCsLinkName.terms;

@Singleton
public class ModsMenu extends BlurredBackgroundDialog implements DisplaysText {

	private static final float VERSION_WIDTH = 240;
	private static final float COMPATIBILITY_WIDTH = 280;
	private final MenuSkin menuSkin;
	private final MainGameSkin mainGameSkin;
	private final ManagementSkin managementSkin;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final TooltipFactory tooltipFactory;
	private final LocalModRepository modRepository;
	private final ModCompatibilityChecker modCompatibilityChecker;
	private final MenuButtonFactory menuButtonFactory;
	private final ModioAuthManager modioAuthManager;
	private final SteamUserManager steamUserManager;
	private final ModioTermsConditions modioTermsConditions;
	private Container<TextButton> refreshButtonContainer;
	private Container<TextButton> submitEmailButton;
	private Container<TextButton> submitCodeButton;

	private boolean showingEmailLogin;
	private boolean emailCodeRequested;
	private boolean errorEncountered;

	private boolean showingTerms;
	private TermsConditionsContent termsConditionsContent;

	@Inject
	public ModsMenu(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
					I18nTranslator i18nTranslator, TooltipFactory tooltipFactory, SoundAssetDictionary soundAssetDictionary,
					LocalModRepository modRepository, ModCompatibilityChecker modCompatibilityChecker,
					MenuButtonFactory menuButtonFactory, ModioAuthManager modioAuthManager, SteamUserManager steamUserManager, ModioTermsConditions modioTermsConditions) {
		super(I18nText.BLANK, guiSkinRepository.getMenuSkin(), messageDispatcher, soundAssetDictionary);
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.mainGameSkin = guiSkinRepository.getMainGameSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.tooltipFactory = tooltipFactory;
		this.modRepository = modRepository;
		this.modCompatibilityChecker = modCompatibilityChecker;
		this.menuButtonFactory = menuButtonFactory;
		this.modioAuthManager = modioAuthManager;
		this.steamUserManager = steamUserManager;
		this.modioTermsConditions = modioTermsConditions;
	}

	@Override
	public void dispose() {

	}

	@Override
	public void rebuildUI() {
		contentTable.clear();


		Table mainTable = new Table();
		mainTable.defaults().padLeft(120f).padRight(120f).spaceBottom(40f);
		mainTable.center();
		mainTable.setBackground(menuSkin.getDrawable("asset_square_bg"));

		Label titleRibbon = new Label(i18nTranslator.translate("MENU.MODS"), menuSkin, "key_bindings_title_ribbon");
		titleRibbon.setAlignment(Align.center);
		mainTable.add(titleRibbon).spaceTop(28f).row();

		if (showingEmailLogin) {
			buildEmailLoginUI(mainTable);
		} else if (showingTerms) {
			buildTermsConditionsUI(mainTable);
		} else {
			Table modsTable = modsTable();
			modsTable.top();

			ScrollPane scrollPane = new EnhancedScrollPane(modsTable, menuSkin);
			scrollPane.setForceScroll(false, true);
			scrollPane.setFadeScrollBars(false);
			scrollPane.setScrollbarsVisible(true);
			scrollPane.setScrollBarPositions(true, true);

			Label nameHeader = new Label(i18nTranslator.translate("MODS.TABLE.NAME"), menuSkin, "mod_table_header_label");
			nameHeader.setAlignment(Align.center);
			Label versionHeader = new Label(i18nTranslator.translate("MODS.TABLE.VERSION"), menuSkin, "mod_table_header_label");
			versionHeader.setAlignment(Align.center);
			Label compatibilityHeader = new Label(i18nTranslator.translate("MODS.TABLE.COMPATIBILITY"), menuSkin, "mod_table_header_label");
			compatibilityHeader.setAlignment(Align.center);
			Label enabledHeader = new Label(i18nTranslator.translate("MODS.TABLE.ENABLED"), menuSkin, "mod_table_header_label");
			enabledHeader.setAlignment(Align.center);

			modsTable.layout();
			float firstColumnMinWidth = modsTable.getCells().get(0).getMinWidth();

			Table modsTableHeader = new Table();
			modsTableHeader.left();
			modsTableHeader.setBackground(menuSkin.getDrawable("asset_long_banner"));
			modsTableHeader.add(nameHeader).minWidth(firstColumnMinWidth);
			modsTableHeader.add(versionHeader).width(VERSION_WIDTH).spaceLeft(76).spaceRight(76);
			modsTableHeader.add(compatibilityHeader).width(COMPATIBILITY_WIDTH).spaceLeft(76).padRight(76);
			modsTableHeader.add(enabledHeader);

			Table headerButtonsTable = new Table();
			headerButtonsTable.defaults().padRight(40);
			headerButtonsTable.left();

			Container<TextButton> browseButton = menuButtonFactory.createButton("MODS.BUTTON.BROWSE", menuSkin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
					.withScaledToFitLabel(554)
					.withAction(() -> Gdx.net.openURI(modioAuthManager.getModioGameHomepage()))
					.build();
			refreshButtonContainer = menuButtonFactory.createButton("MODS.BUTTON.REFRESH", menuSkin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
					.withScaledToFitLabel(554)
					.withAction(this::beginRefreshMods)
					.build();

			headerButtonsTable.add(browseButton).left();
			headerButtonsTable.add(refreshButtonContainer).left();
			headerButtonsTable.add(new Container<>()).growX();

			mainTable.add(headerButtonsTable).growX().row();
			mainTable.add(modsTableHeader).spaceBottom(32).row();
			mainTable.add(scrollPane).width(modsTableHeader.getBackground().getMinWidth() + 20).height(1256f).row(); //TODO: revisit this to use a 9-patch background and not explicitly set height
		}

		contentTable.add(mainTable);
	}

	private void buildEmailLoginUI(Table mainTable) {
		mainTable.add(new Label(i18nTranslator.getTranslatedString("MODS.EMAIL_LOGIN.INFO").toString(), menuSkin, "mod_table_value_label")).row();
		mainTable.add(new Label(i18nTranslator.getTranslatedString("MODS.EMAIL_LOGIN.EMAIL").toString(), menuSkin, "mod_table_value_label")).row();

		TextField emailInput = new TextField("", menuSkin);
		emailInput.addListener(new ChangeCursorOnHover(emailInput, GameCursor.I_BEAM, messageDispatcher));
		emailInput.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		TextField codeInput = new TextField("", menuSkin);
		codeInput.addListener(new ChangeCursorOnHover(emailInput, GameCursor.I_BEAM, messageDispatcher));
		codeInput.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));

		Table emailRow = new Table();
		emailRow.add(new Label(i18nTranslator.getTranslatedString("MODS.EMAIL_LOGIN.EMAIL_LABEL").toString(), menuSkin, "mod_table_value_label")).padRight(20);
		emailRow.add(emailInput).width(800);
		mainTable.add(emailRow).row();

		submitEmailButton = menuButtonFactory.createButton("MODS.EMAIL_LOGIN.REQUEST_CODE", menuSkin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
				.withScaledToFitLabel(554)
				.withAction(() -> {
					if (!emailInput.getText().isEmpty()) {
						submitEmailButton.getActor().setDisabled(true);
						modioAuthManager.requestEmailCode(emailInput.getText(), new Callback() {
							@Override
							public void onFailure(Call call, IOException e) {
								Logger.error("Failed request to mod.io to request email code", e);
								errorEncountered = true;
								submitEmailButton.getActor().setDisabled(false);
								rebuildUI();
							}

							@Override
							public void onResponse(Call call, Response response) throws IOException {
								submitEmailButton.getActor().setDisabled(false);
								if (response.isSuccessful()) {
									emailCodeRequested = true;
								} else {
									Logger.error("Failed request to mod.io to request email code: " + response);
									errorEncountered = true;
								}
								rebuildUI();
							}
						});
					}
				})
				.build();
		mainTable.add(submitEmailButton).row();

		if (emailCodeRequested) {
			mainTable.add(new Label(i18nTranslator.getTranslatedString("MODS.EMAIL_LOGIN.CHECK_EMAIL").toString(), menuSkin, "mod_table_value_label")).row();

			Table codeRow = new Table();
			codeRow.add(new Label(i18nTranslator.getTranslatedString("MODS.EMAIL_LOGIN.CODE_LABEL").toString(), menuSkin, "mod_table_value_label")).padRight(20);
			codeRow.add(codeInput).width(800);
			mainTable.add(codeRow).row();

			submitCodeButton = menuButtonFactory.createButton("MODS.EMAIL_LOGIN.SUBMIT_CODE", menuSkin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
					.withScaledToFitLabel(554)
					.withAction(() -> {
						if (!codeInput.getText().isEmpty()) {
							submitCodeButton.getActor().setDisabled(true);
							modioAuthManager.submitEmailCode(codeInput.getText(), () -> {
								submitCodeButton.getActor().setDisabled(false);
								if (modioAuthManager.isUserAuthenticated()) {
									showingEmailLogin = false;
									messageDispatcher.dispatchMessage(MessageType.REFRESH_MOD_FILES); // this should cause the UI to change again
								} else {
									errorEncountered = true;
									rebuildUI();
								}
							});
						}
					})
					.build();
			mainTable.add(submitCodeButton).row();
		}

		if (errorEncountered) {
			Label errorLabel = new Label(i18nTranslator.getTranslatedString("MODS.EMAIL_LOGIN.ERROR").toString(), menuSkin, "mod_table_value_label");
			errorLabel.setWrap(true);
			mainTable.add(errorLabel).row();
		}

		mainTable.add(new Container<>()).growY().row();
	}

	private void buildTermsConditionsUI(Table mainTable) {
		Label plainTextLabel = new Label(termsConditionsContent.getPlaintext(), menuSkin, "mod_table_value_label");
		plainTextLabel.setWrap(true);
		mainTable.add(plainTextLabel).width(1600f).row();

		Table buttonsTable = new Table();
		Container<TextButton> agreeButton = menuButtonFactory.createButton(termsConditionsContent.getButtons().get(agree).getText(),
						menuSkin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
				.withScaledToFitLabel(554)
				.withAction(() -> {
					modioTermsConditions.setTermsAccepted(true);
					beginRefreshMods();
				})
				.build();
		buttonsTable.add(agreeButton);
		Container<TextButton> disagreeButton = menuButtonFactory.createButton(termsConditionsContent.getButtons().get(disagree).getText(),
						menuSkin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
				.withScaledToFitLabel(554)
				.withAction(() -> {
					modioTermsConditions.setTermsAccepted(false);
					showingEmailLogin = false;
					showingTerms = false;
					rebuildUI();
				})
				.build();
		buttonsTable.add(disagreeButton).padLeft(20);
		mainTable.add(buttonsTable).row();

		Table linksTable = new Table();
		linksTable.defaults().padLeft(10).padRight(10);
		Label termsLink = new Label(termsConditionsContent.getLinks().get(terms).getText(), menuSkin, "mod_table_value_label");
		termsLink.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.net.openURI(termsConditionsContent.getLinks().get(terms).getUrl());
			}
		});
		termsLink.addListener(new ChangeCursorOnHover(termsLink, GameCursor.SELECT, messageDispatcher));
		termsLink.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		linksTable.add(termsLink);
		linksTable.add(new Label("-", menuSkin, "mod_table_value_label"));
		Label privacyLink = new Label(termsConditionsContent.getLinks().get(privacy).getText(), menuSkin, "mod_table_value_label");
		privacyLink.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.net.openURI(termsConditionsContent.getLinks().get(privacy).getUrl());
			}
		});
		privacyLink.addListener(new ChangeCursorOnHover(privacyLink, GameCursor.SELECT, messageDispatcher));
		privacyLink.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		linksTable.add(privacyLink);

		mainTable.add(linksTable).row();

		if (errorEncountered) {
			Label errorLabel = new Label(i18nTranslator.getTranslatedString("MODS.EMAIL_LOGIN.ERROR").toString(), menuSkin, "mod_table_value_label");
			errorLabel.setWrap(true);
			mainTable.add(errorLabel).row();
		}

		mainTable.add(new Container<>()).growY().row();
	}

	@Override
	public void show(Stage stage) {
		super.show(stage);
		if (modioAuthManager.isUserAuthenticated()) {
			beginRefreshMods();
		}
		rebuildUI();
	}

	@Override
	public void close() {
		super.close();
		this.showingEmailLogin = false;
		this.emailCodeRequested = false;
		this.errorEncountered = false;

		if (modRepository.hasChangesToApply()) {
			messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_INFO, InfoType.MOD_CHANGES_OUTSTANDING);
		}
	}

	private Table modsTable() {
		final List<ParsedMod> activeMods = modRepository.getActiveMods();
		Collections.reverse(activeMods);
		final List<ParsedMod> inactiveMods = new ArrayList<>();
		for (ParsedMod mod : modRepository.getAll()) {
			if (!activeMods.contains(mod)) {
				inactiveMods.add(mod);
			}
		}
		inactiveMods.sort(Comparator.comparing(o -> o.getInfo().getName()));

		Map<Boolean, List<ParsedMod>> splitByBaseMod = activeMods.stream().collect(Collectors.partitioningBy(mod -> mod.getInfo().isBaseMod()));
		final List<ParsedMod> modsInOrder = new ArrayList<>();
		modsInOrder.addAll(splitByBaseMod.getOrDefault(false, Collections.emptyList()));
		modsInOrder.addAll(inactiveMods);
		modsInOrder.addAll(splitByBaseMod.getOrDefault(true, Collections.emptyList()));


		DragAndDrop dragAndDrop = new DragAndDrop();

		Table table = new Table();

		for (int i = 0; i < modsInOrder.size(); i++) {
			final int index = i;
			ParsedMod mod = modsInOrder.get(index);
			final boolean isBaseMod = mod.getInfo().isBaseMod();

			CompatibilityResult compatibilityResult = modCompatibilityChecker.checkCompatibility(mod);
			ModCompatibilityChecker.Compatibility compatibility = compatibilityResult.compatibility();

			Label draggableMod = new Label(mod.getInfo().getName(), menuSkin, "draggable_mod");
			draggableMod.setAlignment(Align.center);
			draggableMod.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
			draggableMod.addListener(new ChangeCursorOnHover(draggableMod, GameCursor.REORDER_VERTICAL, messageDispatcher));
			Label versionLabel = new Label(mod.getInfo().getVersion().toString(), menuSkin, "mod_table_value_label");
			versionLabel.setAlignment(Align.center);
			versionLabel.layout();
			if (versionLabel.getPrefWidth() > VERSION_WIDTH) {
				tooltipFactory.simpleTooltip(versionLabel, new I18nText(mod.getInfo().getVersion().toString()), TooltipLocationHint.ABOVE);
				versionLabel.setEllipsis(true);
			}
			Label compatibleLabel = new Label(i18nTranslator.translate(compatibility.getI18nKey()), menuSkin, "mod_table_value_label");
			compatibleLabel.setAlignment(Align.center);
			Button enabledCheckbox = new Button(menuSkin, "checkbox");
			enabledCheckbox.setChecked(activeMods.contains(mod));
			enabledCheckbox.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
			enabledCheckbox.addListener(new ChangeCursorOnHover(enabledCheckbox, GameCursor.SELECT, messageDispatcher));
			enabledCheckbox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (enabledCheckbox.isChecked()) {
						activeMods.add(mod);
					} else {
						activeMods.remove(mod);
					}
					orderChanged(modsInOrder);
				}
			});

			Button homepageButton = new Button(menuSkin, "btn_homepage");

			if (StringUtils.isBlank(mod.getInfo().getHomepageUrl())) {
				homepageButton.getColor().a = 0.6f;
				tooltipFactory.simpleTooltip(homepageButton, "MODS.MISSING_HOMEPAGE_URL", TooltipLocationHint.ABOVE);
			} else {
				homepageButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
				homepageButton.addListener(new ChangeCursorOnHover(homepageButton, GameCursor.SELECT, messageDispatcher));
				homepageButton.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						Gdx.net.openURI(mod.getInfo().getHomepageUrl());
					}
				});
			}


			if (isBaseMod) {
				disable(enabledCheckbox);
			} else if (compatibility == ModCompatibilityChecker.Compatibility.INCOMPATIBLE) {
				disable(enabledCheckbox);
			}

			Container<Label> draggableModContainer = new Container<>(draggableMod);
			Container<Label> versionLabelContainer = new Container<>(versionLabel);
			Container<Label> compatibleLabelContainer = new Container<>(compatibleLabel);
			Container<Button> enabledCheckboxContainer = new Container<>(enabledCheckbox);
			Container<Button> homepageButtonContainer = new Container<>(homepageButton);

			if (menuSkin.getDrawable("btn_mods_draggable") instanceof NinePatchDrawable ninePatchDrawable) {
				float middleWidth = ninePatchDrawable.getPatch().getMiddleWidth();

				if (draggableModContainer.getPrefWidth() > middleWidth) {
					draggableMod.setEllipsis(true);
					draggableModContainer.width(middleWidth);
				}
			}

			Table tooltipTable = new Table();
			Label headerLabel = new Label(mod.getInfo().getName(), mainGameSkin.get("complex-tooltip-header", Label.LabelStyle.class));
			headerLabel.setWrap(true);
			tooltipTable.add(headerLabel).width(700).center().row();

			Label descriptionLabel = new Label(mod.getInfo().getDescription(), mainGameSkin);
			descriptionLabel.setWrap(true);
			tooltipTable.add(descriptionLabel).width(700).center().row();

			if (compatibility == ModCompatibilityChecker.Compatibility.INCOMPATIBLE) {
				I18nText modIncompatabilityText = i18nTranslator.getTranslatedWordWithReplacements("MODS.INCOMPATIBLE_VERSION_DESCRIPTION", Map.of(
						"modGameVersion", new I18nWord(mod.getInfo().getGameVersion().toString()),
						"gameVersion", new I18nWord(GlobalSettings.VERSION.toString())
				));

				Label modIncompatibilityLabel = new Label(modIncompatabilityText.toString(), mainGameSkin);
				modIncompatibilityLabel.setWrap(true);
				tooltipTable.add(modIncompatibilityLabel).padTop(10).width(700).center().row();
			}

			tooltipFactory.complexTooltip(draggableModContainer, tooltipTable, TooltipFactory.TooltipBackground.LARGE_PATCH_DARK);

			draggableModContainer.padBottom(14);
			versionLabelContainer.padLeft(76);
			versionLabelContainer.padRight(76);
			versionLabelContainer.width(VERSION_WIDTH);

			compatibleLabelContainer.width(COMPATIBILITY_WIDTH);

			enabledCheckboxContainer.padLeft(76);
			enabledCheckboxContainer.padRight(76);
			homepageButtonContainer.padRight(34);

			//dirty bodge to show tooltip and pretend the mod is disabled
			if (isBaseMod || !enabledCheckbox.isChecked() || compatibility == ModCompatibilityChecker.Compatibility.INCOMPATIBLE) {
				draggableMod.getColor().a = 0.6f;
			} else {

				List<Container<?>> rowTarget = List.of(draggableModContainer,
						versionLabelContainer,
						compatibleLabelContainer,
						enabledCheckboxContainer,
						homepageButtonContainer);
				dragAndDrop.addSource(new DraggableModSource(dragAndDrop, draggableMod, index));
				rowTarget.forEach(t -> {
					dragAndDrop.addTarget(new DraggableModTarget(t, rowTarget, modsInOrder, index));
				});
			}


			table.add(draggableModContainer).fill();
			table.add(versionLabelContainer).fill();
			table.add(compatibleLabelContainer).fill().expandX();
			table.add(enabledCheckboxContainer).fill();
			table.add(homepageButtonContainer).fill();
			table.row();
		}


		return table;
	}

	private void beginRefreshMods() {
//		refreshButtonContainer.getActor().setDisabled(true);

		if (!modioAuthManager.isUserAuthenticated()) {
			if (GlobalSettings.DEV_MODE) {
				Logger.debug("Not authenticated with Mod.io");
			}
			if (SteamAPI.isSteamRunning() && steamUserManager.isEncryptedAppTicketReady()) {
				if (GlobalSettings.DEV_MODE) {
					Logger.debug("Attempting authentication with Steam");
				}
				modioAuthManager.authenticateWithSteam(steamUserManager.getEncryptedAppTicket(), modioTermsConditions.isTermsAccepted(), () -> {
					// on success
					if (GlobalSettings.DEV_MODE) {
						Logger.debug("Steam authentication with with Mod.io successful");
					}
					doRefreshMods();
				}, () -> {
					// on error, removing app ticket and calling this again to fall into email login path
					if (GlobalSettings.DEV_MODE) {
						Logger.debug("Steam authentication with with Mod.io failed, clearing app ticket");
					}
					steamUserManager.clearEncryptedAppTicket();
					beginRefreshMods();
				}, () -> {
					// on terms required
					if (GlobalSettings.DEV_MODE) {
						Logger.debug("Steam authentication requires mod.io T&Cs acceptance");
					}
					modioTermsConditions.getTermsConditionsContent((terms) -> {
						ModsMenu.this.termsConditionsContent = terms;
						this.showingTerms = true;
						rebuildUI();
					}, () -> {
						this.errorEncountered = true;
						rebuildUI();
					});
				});
			} else {
				// User not authenticated and no steam app ticket
				// Call T&Cs as a heartbeat to see if we are online
				modioTermsConditions.getTermsConditionsContent((terms) -> {
					// Can communicate with mod.io so use email code auth
					this.showingEmailLogin = true;
					rebuildUI();
				}, () -> {
					if (GlobalSettings.DEV_MODE) {
						Logger.debug("Could not communicate with mod.io API, refreshing local mods only");
					}
					refreshLocalModsOnly();
				});
			}
		} else {
			if (GlobalSettings.DEV_MODE) {
				Logger.debug("Authenticated with Mod.io, refreshing mods");
			}
			doRefreshMods();
		}
	}

	private void doRefreshMods() {
		if (GlobalSettings.DEV_MODE) {
			Logger.debug("TODO: Full refresh of mods");
		}
	}

	private void refreshLocalModsOnly() {
		if (GlobalSettings.DEV_MODE) {
			Logger.debug("TODO: Local only refresh of mods");
		}
	}

	private class DraggableModSource extends DragAndDrop.Source {
		private final DragAndDrop dragAndDrop;
		private final Label originalLabel;
		private final int index;


		DraggableModSource(DragAndDrop dragAndDrop, Label originalLabel, int index) {
			super(originalLabel);
			this.dragAndDrop = dragAndDrop;
			this.originalLabel = originalLabel;
			this.index = index;
		}

		@Override
		public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
			Label dragActor = new Label(originalLabel.getText(), originalLabel.getStyle());
			dragActor.setAlignment(originalLabel.getLabelAlign());

			DragAndDrop.Payload payload = new DragAndDrop.Payload();
			payload.setDragActor(dragActor);
			dragAndDrop.setDragActorPosition(originalLabel.getWidth() - x, -dragActor.getHeight() / 2);
			return payload;
		}
	}

	private class DraggableModTarget extends DragAndDrop.Target {
		private final List<Container<?>> wholeRow;
		private final List<ParsedMod> modsInOrder;
		private final int index;

		public DraggableModTarget(Actor actor, List<Container<?>> wholeRow, List<ParsedMod> modsInOrder, int index) {
			super(actor);
			this.wholeRow = wholeRow;
			this.modsInOrder = modsInOrder;
			this.index = index;
		}

		@Override
		public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
			wholeRow.forEach(c -> c.setBackground(managementSkin.getDrawable("drag_over_tint")));
			return true;
		}

		@Override
		public void reset(DragAndDrop.Source source, DragAndDrop.Payload payload) {
			super.reset(source, payload);
			wholeRow.forEach(c -> c.setBackground(null));
		}

		@Override
		public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
			if (source instanceof DraggableModSource draggableModSource) {
				Collections.swap(modsInOrder, index, draggableModSource.index);
				orderChanged(modsInOrder);
			}
		}
	}

	private void disable(Actor actor) {
		actor.setTouchable(Touchable.disabled);
		actor.getColor().a = 0.6f;
	}

	private void orderChanged(List<ParsedMod> modsInOrder) {
		List<ParsedMod> currentActiveMods = modRepository.getActiveMods();
		List<ParsedMod> newActiveMods = new ArrayList<>();

		for (ParsedMod mod : modsInOrder) {
			if (currentActiveMods.contains(mod)) {
				newActiveMods.add(mod);
			}
		}

		Collections.reverse(newActiveMods);
		modRepository.setActiveMods(newActiveMods);

		this.rebuildUI();
	}

}
