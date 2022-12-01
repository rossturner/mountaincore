package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.InfoType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.async.ErrorType;
import technology.rocketjump.saul.settlement.notifications.Notification;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nString;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.tooltips.I18nTextElement;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class GameDialogDictionary implements DisplaysText, GameContextAware {

	private final I18nTranslator translator;
	private final Skin uiSkin;
	private final MessageDispatcher messageDispatcher;
	private final SoundAssetDictionary soundAssetDictionary;

	private final Map<ErrorType, ModalDialog> byErrorType = new EnumMap<>(ErrorType.class);
	private final Map<InfoType, ModalDialog> byInfoType = new EnumMap<>(InfoType.class);
	private GameContext gameContext;

	@Inject
	public GameDialogDictionary(I18nTranslator translator, GuiSkinRepository guiSkinRepository,
	                            MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
		this.translator = translator;
		this.uiSkin = guiSkinRepository.getMenuSkin();
		this.messageDispatcher = messageDispatcher;
		this.soundAssetDictionary = soundAssetDictionary;

		createDialogs();
	}

	public ModalDialog getErrorDialog(ErrorType errorType) {
		return byErrorType.get(errorType);
	}

	public ModalDialog getInfoDialog(InfoType infoType) {
		return byInfoType.get(infoType);
	}

	@Override
	public void rebuildUI() {
		byErrorType.clear();
		createDialogs();
	}

	private void createDialogs() {
		for (ErrorType errorType : ErrorType.values()) {
			ModalDialog dialog = create(errorType);
			byErrorType.put(errorType, dialog);
		}


		for (InfoType infoType : InfoType.values()) {
			ModalDialog dialog = create(infoType);
			byInfoType.put(infoType, dialog);
		}

	}

	private ModalDialog create(ErrorType errorType) {
		I18nText title = translator.getTranslatedString("GUI.DIALOG.ERROR_TITLE");
		I18nText descriptionText = translator.getTranslatedString(errorType.i18nKey).breakAfterLength(translator.getCurrentLanguageType().getBreakAfterLineLength());
		I18nText buttonText = translator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

		return new ModalDialog(title, descriptionText, buttonText, uiSkin, messageDispatcher, soundAssetDictionary);
	}

	private ModalDialog create(InfoType infoType) {
		I18nText title = translator.getTranslatedString("GUI.DIALOG.INFO_TITLE");
		I18nText descriptionText = translator.getTranslatedString(infoType.i18nKey).breakAfterLength(translator.getCurrentLanguageType().getBreakAfterLineLength());
		I18nText buttonText = translator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

		return new ModalDialog(title, descriptionText, buttonText, uiSkin, messageDispatcher, soundAssetDictionary);
	}

	public NotificationDialog create(Notification notification) {
		I18nText title = translator.getTranslatedString(notification.getType().getI18nTitleKey());

		Map<String, I18nString> replacements = new HashMap<>();
		for (Map.Entry<String, I18nString> replacement : notification.getTextReplacements()) {
			replacements.put(replacement.getKey(), replacement.getValue());
		}
		I18nText descriptionText = translator.getTranslatedWordWithReplacements(notification.getType().getI18nDescriptionKey(), replacements);

		I18nText dismissText = translator.getTranslatedString("GUI.DIALOG.DISMISS");

		NotificationDialog notificationDialog = new NotificationDialog(title, descriptionText, notification.getType().getImageFilename(), uiSkin, messageDispatcher, soundAssetDictionary);

		if (notification.getWorldPosition() != null || notification.getSelectableTarget() != null) {
			I18nText jumpToText = translator.getTranslatedString("GUI.DIALOG.JUMP_TO");
			notificationDialog.withButton(jumpToText, () -> {
				if (notification.getSelectableTarget() != null) {
					messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, notification.getSelectableTarget().getPosition());
					messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, notification.getSelectableTarget());
				} else {
					messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, notification.getWorldPosition());
				}
			});
		}
		notificationDialog.withButton(translator.getTranslatedString("GUI.DIALOG.HIDE_FURTHER_NOTIFICATIONS"), () -> {
			gameContext.getSettlementState().suppressedNotificationTypes.add(notification.getType());
		});
		notificationDialog.withButton(dismissText);

		// TODO stop this kind of notification

		return notificationDialog;
	}

	// Might want to extract out a more generic "show a dialog with some replaced items" method
	public ModalDialog createModsMissingDialog(List<String> missingModNames) {
		I18nText title = translator.getTranslatedString("GUI.DIALOG.INFO_TITLE");
		I18nText descriptionText = translator.getTranslatedString("MODS.MISSING_MODS_DIALOG_TEXT").breakAfterLength(translator.getCurrentLanguageType().getBreakAfterLineLength());
		descriptionText.getElements().add(I18nTextElement.lineBreak);
		descriptionText.getElements().add(I18nTextElement.lineBreak);
		for (String missingModName : missingModNames) {
			descriptionText.getElements().add(new I18nTextElement("- " + missingModName, null));
			descriptionText.getElements().add(I18nTextElement.lineBreak);
		}
		I18nText buttonText = translator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

		return new ModalDialog(title, descriptionText, buttonText, uiSkin, messageDispatcher, soundAssetDictionary);
	}

	public ModalDialog createModsMissingSaveExceptionDialog(List<String> missingModNames) {
		I18nText title = translator.getTranslatedString("GUI.DIALOG.ERROR_TITLE");
		I18nText descriptionText = translator.getTranslatedString("MODS.MISSING_MODS_SAVE_EXCEPTION_DIALOG_TEXT").breakAfterLength(translator.getCurrentLanguageType().getBreakAfterLineLength());
		descriptionText.getElements().add(I18nTextElement.lineBreak);
		descriptionText.getElements().add(I18nTextElement.lineBreak);
		for (String missingModName : missingModNames) {
			descriptionText.getElements().add(new I18nTextElement("- " + missingModName, null));
			descriptionText.getElements().add(I18nTextElement.lineBreak);
		}
		I18nText buttonText = translator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

		return new ModalDialog(title, descriptionText, buttonText, uiSkin, messageDispatcher, soundAssetDictionary);
	}

	static String[] splitLines(String translatedString) {
		if (translatedString.length() < 10) {
			return new String[] {translatedString};
		}
		int midpointCursor = translatedString.length() / 2 + 1;
		for (int cursor = midpointCursor; cursor > 0; cursor--) {
			if (translatedString.charAt(cursor) == ' ') {
				return new String[] {translatedString.substring(0, cursor) , translatedString.substring(cursor + 1)};
			}

		}
		return new String[] {};
	}

	public GameDialog createInfoDialog(Skin skin, InfoType infoType, Map<String, I18nString> replacements) {
		NoTitleDialog dialog = new NoTitleDialog(skin, messageDispatcher, soundAssetDictionary);
		I18nText translatedString = translator.getTranslatedWordWithReplacements(infoType.i18nKey, replacements);
		I18nText descriptionText = translatedString.breakAfterLength(translator.getCurrentLanguageType().getBreakAfterLineLength());
		dialog.getContentTable().add(new Label(descriptionText.toString(), skin, "white_text_default-font-23")).padRight(180f).padLeft(180f).growY();
		return dialog;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
