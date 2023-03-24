package technology.rocketjump.mountaincore.rooms;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.io.IOException;

@Singleton
public class RoomTypeI18nProcessor implements DisplaysText {

	private final RoomTypeDictionary roomTypeDictionary;
	private final I18nTranslator translator;

	@Inject
	private RoomTypeI18nProcessor(RoomTypeDictionary roomTypeDictionary, I18nTranslator translator) throws IOException {
		this.roomTypeDictionary = roomTypeDictionary;
		this.translator = translator;
		rebuildUI();
	}

	@Override
	public void rebuildUI() {
		roomTypeDictionary.byTranslatedName.clear();
		for (RoomType roomType : roomTypeDictionary.getAll()) {
			roomType.setI18nValue(translator.getTranslatedString(roomType.getI18nKey()));
			roomTypeDictionary.byTranslatedName.put(roomType.getI18nValue().toString(), roomType);
		}
	}
}
