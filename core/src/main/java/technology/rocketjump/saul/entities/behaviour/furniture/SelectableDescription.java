package technology.rocketjump.saul.entities.behaviour.furniture;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.util.List;

public interface SelectableDescription {

	List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext, MessageDispatcher messageDispatcher);

}
