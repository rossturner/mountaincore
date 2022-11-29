package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MenuSkin;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LabelFactory {
	private final I18nTranslator i18nTranslator;
	private final MenuSkin menuSkin;

	@Inject
	public LabelFactory(I18nTranslator i18nTranslator, GuiSkinRepository skinRepository) {
		this.i18nTranslator = i18nTranslator;
		this.menuSkin = skinRepository.getMenuSkin();
	}

	public Label titleRibbon(String i18nKey) {
		Label label = new Label(i18nTranslator.translate(i18nKey), menuSkin, "title_ribbon");
		label.setAlignment(Align.center);
		return label;
	}
}
