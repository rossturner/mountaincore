package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.military.model.SquadOrderType;

public class SquadOrderChangeMessage {

	public final Squad squad;
	public final SquadOrderType newOrderType;

	public SquadOrderChangeMessage(Squad squad, SquadOrderType newOrderType) {
		this.squad = squad;
		this.newOrderType = newOrderType;
	}
}
