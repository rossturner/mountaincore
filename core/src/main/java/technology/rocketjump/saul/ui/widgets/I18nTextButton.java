package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import technology.rocketjump.saul.ui.i18n.I18nWordClass;

public class I18nTextButton extends TextButton {

	private String i18nKey;
	private final I18nWordClass i18nWordClass;

	public I18nTextButton(String i18nKey, String i18nValue, Skin skin) {
		this(i18nKey, i18nValue, skin, I18nWordClass.UNSPECIFIED);
	}

	public I18nTextButton(String i18nKey, String i18nValue, Skin skin, I18nWordClass i18nWordClass) {
		super(i18nValue, skin);
		this.i18nKey = i18nKey;
		this.i18nWordClass = i18nWordClass;
	}

	public void setText(String i18nKey, String i18nValue) {
		this.i18nKey = i18nKey;
		setText(i18nValue);
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public I18nWordClass getI18nWordClass() {
		return i18nWordClass;
	}
}
