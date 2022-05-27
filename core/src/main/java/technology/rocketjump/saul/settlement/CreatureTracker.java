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
public class CreatureTracker implements GameContextAware {

	private final Map<Long, Entity> byId = new HashMap<>();
	private final Map<Long, Entity> livingCreatures = new HashMap<>();
	private final Map<Long, Entity> deadCreatures = new HashMap<>();

	public void creatureAdded(Entity entity) {
		if (entity.getLocationComponent().isUntracked()) {
			return;
		}

		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getConsciousness().equals(Consciousness.DEAD)) {
			deadCreatures.put(entity.getId(), entity);
		} else {
			livingCreatures.put(entity.getId(), entity);
		}
		byId.put(entity.getId(), entity);
	}

	public void creatureRemoved(Entity entity) {
		byId.remove(entity.getId());
		livingCreatures.remove(entity.getId());
		deadCreatures.remove(entity.getId());
	}

	public void creatureDied(Entity entity) {
		livingCreatures.remove(entity.getId());
		deadCreatures.put(entity.getId(), entity);
	}

	public Collection<Entity> getAll() {
		return byId.values();
	}


	public Collection<Entity> getLiving() {
		return livingCreatures.values();
	}

	public Collection<Entity> getDead() {
		return deadCreatures.values();
	}

	public int count() {
		return livingCreatures.size();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		// Does not use GameContext, only needs to clearContextRelatedState state
	}

	@Override
	public void clearContextRelatedState() {
		byId.clear();
		livingCreatures.clear();
		deadCreatures.clear();
	}

}
