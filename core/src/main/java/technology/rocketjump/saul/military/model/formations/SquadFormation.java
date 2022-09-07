package technology.rocketjump.saul.military.model.formations;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.gamecontext.GameContext;

public interface SquadFormation {

	String getFormationName();

	GridPoint2 getFormationPosition(int squadMemberIndex, GridPoint2 centralLocation, GameContext gameContext, int totalSquadMembers);

}
