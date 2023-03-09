package technology.rocketjump.saul.ui;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import technology.rocketjump.saul.materials.GameMaterialI18nUpdater;
import technology.rocketjump.saul.messaging.InfoType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.fonts.OnDemandFontRepository;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.widgets.text.DecoratedStringFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class keeps track of all DisplaysText implementations for notifying of language/font changes
 */
@Singleton
public class DisplaysTextRegister implements Telegraph {

	private final GameMaterialI18nUpdater gameMaterialI18nUpdater;
	private final Map<String, DisplaysText> registered = new HashMap<>();
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;
	private final OnDemandFontRepository onDemandFontRepository;
	private final DecoratedStringFactory decoratedStringFactory;

	@Inject
	public DisplaysTextRegister(MessageDispatcher messageDispatcher, GameMaterialI18nUpdater gameMaterialI18nUpdater,
								I18nTranslator i18nTranslator, OnDemandFontRepository onDemandFontRepository,
								DecoratedStringFactory decoratedStringFactory) {
		this.gameMaterialI18nUpdater = gameMaterialI18nUpdater;
		this.i18nTranslator = i18nTranslator;
		this.messageDispatcher = messageDispatcher;
		this.onDemandFontRepository = onDemandFontRepository;
		this.decoratedStringFactory = decoratedStringFactory;

		messageDispatcher.addListener(this, MessageType.LANGUAGE_CHANGED);
		messageDispatcher.addListener(this, MessageType.GUI_SCALE_CHANGED);
	}

	public void registerClasses(Set<Class<? extends DisplaysText>> updatableClasses, Injector injector) {
		for (Class updatableClass : updatableClasses) {
			if (!updatableClass.isInterface()) {
				register((DisplaysText)injector.getInstance(updatableClass));
			}
		}
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.GUI_SCALE_CHANGED: {
				onDemandFontRepository.dispose();
				messageDispatcher.dispatchMessage(MessageType.FONTS_CHANGED);
				for (DisplaysText displaysTextInstance : registered.values()) {
					displaysTextInstance.rebuildUI();
				}
			}
			case MessageType.LANGUAGE_CHANGED: {
				// Add any PRE-LANGUAGE CHANGED stuff here
				i18nTranslator.preLanguageUpdated();
				decoratedStringFactory.preLanguageUpdated();
				onDemandFontRepository.preLanguageUpdated();
				gameMaterialI18nUpdater.preLanguageUpdated();
				// Then the rebuildUI() callbacks are called
				for (DisplaysText displaysTextInstance : registered.values()) {
					displaysTextInstance.rebuildUI();
				}

				// Post-language changed
				if (!i18nTranslator.getDictionary().isCompleteTranslation()) {
					messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_INFO, InfoType.LANGUAGE_TRANSLATION_INCOMPLETE);
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void register(DisplaysText updatableInstance) {
		String className = updatableInstance.getClass().getName();
		if (registered.containsKey(className)) {
			throw new RuntimeException("Duplicate class registered in " + this.getClass().getName() + ": " + className);
		}
		registered.put(className, updatableInstance);
	}

}
