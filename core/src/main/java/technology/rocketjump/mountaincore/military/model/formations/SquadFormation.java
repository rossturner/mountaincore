package technology.rocketjump.mountaincore.military.model.formations;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.util.List;

public interface SquadFormation extends SelectableDescription {

	String getFormationName();

	GridPoint2 getFormationPosition(int squadMemberIndex, GridPoint2 centralLocation, GameContext gameContext, int totalSquadMembers);

	String getI18nKey();

	String getDrawableIconName();

	@Override
	default List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext, MessageDispatcher messageDispatcher) {
		return List.of(i18nTranslator.getTranslatedString(getI18nKey()));
	}
}
