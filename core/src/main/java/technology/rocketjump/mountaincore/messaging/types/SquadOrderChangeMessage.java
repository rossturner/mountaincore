package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.military.model.Squad;
import technology.rocketjump.mountaincore.military.model.SquadOrderType;

public class SquadOrderChangeMessage {

	public final Squad squad;
	public final SquadOrderType newOrderType;

	public SquadOrderChangeMessage(Squad squad, SquadOrderType newOrderType) {
		this.squad = squad;
		this.newOrderType = newOrderType;
	}
}
