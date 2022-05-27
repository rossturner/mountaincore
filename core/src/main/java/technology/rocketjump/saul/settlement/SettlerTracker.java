package technology.rocketjump.saul.settlement;

import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Consciousness;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for keeping track of all items (allocated or not) on the map
 */
@Singleton
public class SettlerTracker implements GameContextAware {

	private final Map<Long, Entity> byId = new HashMap<>();
	private final Map<Long, Entity> livingSettlers = new HashMap<>();
	private final Map<Long, Entity> deadSettlers = new HashMap<>();

	public void settlerAdded(Entity entity) {
		if (entity.getLocationComponent().isUntracked()) {
			return;
		}

		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getConsciousness().equals(Consciousness.DEAD)) {
			deadSettlers.put(entity.getId(), entity);
		} else {
			livingSettlers.put(entity.getId(), entity);
		}
		byId.put(entity.getId(), entity);
	}

	public void settlerRemoved(Entity entity) {
		byId.remove(entity.getId());
		livingSettlers.remove(entity.getId());
		deadSettlers.remove(entity.getId());
	}

	public void settlerDied(Entity entity) {
		livingSettlers.remove(entity.getId());
		deadSettlers.put(entity.getId(), entity);
	}

	public Collection<Entity> getAll() {
		return byId.values();
	}


	public Collection<Entity> getLiving() {
		return livingSettlers.values();
	}

	public Collection<Entity> getDead() {
		return deadSettlers.values();
	}

	public int count() {
		return livingSettlers.size();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		// Does not use GameContext, only needs to clearContextRelatedState state
	}

	@Override
	public void clearContextRelatedState() {
		byId.clear();
		livingSettlers.clear();
		deadSettlers.clear();
	}

}
