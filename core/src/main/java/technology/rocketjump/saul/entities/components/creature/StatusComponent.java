package technology.rocketjump.saul.entities.components.creature;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.InfrequentlyUpdatableComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.status.*;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.*;

public class StatusComponent implements InfrequentlyUpdatableComponent {

	private Entity parentEntity;
	private MessageDispatcher messageDispatcher;
	private GameContext gameContext;

	private final Map<Class<? extends StatusEffect>, StatusEffect> byClassType = new HashMap<>();

	private static Set<Class<? extends StatusEffect>> seriousStatusAilments = Set.of(
			InternalBleeding.class, OnFireStatus.class, VeryHungry.class, VeryThirsty.class,
			DyingOfHunger.class, DyingOfThirst.class
	);

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;

		for (StatusEffect statusEffect : byClassType.values()) {
			statusEffect.setParentEntity(parentEntity);
		}
	}

	@Override
	public void infrequentUpdate(double elapsedTime) {
		for (StatusEffect statusEffect : new ArrayList<>(byClassType.values())) {
			statusEffect.infrequentUpdate(elapsedTime, gameContext, messageDispatcher);
		}
	}

	public void apply(StatusEffect statusEffect) {
		// Should not replace time on current status
		if (!byClassType.containsKey(statusEffect.getClass())) {
			statusEffect.setParentEntity(parentEntity);
			byClassType.put(statusEffect.getClass(), statusEffect);
		}
	}

	public void remove(Class<? extends StatusEffect> statusClass) {
		StatusEffect removed = byClassType.remove(statusClass);
		if (removed != null) {
			removed.onRemoval(gameContext, messageDispatcher);
		}
	}

	public boolean contains(Class<? extends StatusEffect> statusClass) {
		return byClassType.containsKey(statusClass);
	}

	public boolean hasSeriousStatusAilment() {
		for (Class<? extends StatusEffect> currentEffect : byClassType.keySet()) {
			if (seriousStatusAilments.contains(currentEffect)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		StatusComponent clone = new StatusComponent();
		clone.init(parentEntity, messageDispatcher, gameContext);
		for (Map.Entry<Class<? extends StatusEffect>, StatusEffect> entry : byClassType.entrySet()) {
			clone.byClassType.put(entry.getKey(), entry.getValue());
		}
		return clone;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!byClassType.isEmpty()) {
			JSONArray effectsJson = new JSONArray();
			for (StatusEffect statusEffect : byClassType.values()) {
				JSONObject effectJson = new JSONObject(true);
				effectJson.put("_class", statusEffect.getClass().getSimpleName());
				statusEffect.writeTo(effectJson, savedGameStateHolder);
				effectsJson.add(effectJson);
			}
			asJson.put("effects", effectsJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

		JSONArray effectsJson = asJson.getJSONArray("effects");
		if (effectsJson != null) {
			for (int cursor = 0; cursor < effectsJson.size(); cursor++) {
				JSONObject effectJson = effectsJson.getJSONObject(cursor);
				Class<? extends StatusEffect> effectClass = relatedStores.statusEffectDictionary.getByName(effectJson.getString("_class"));
				try {
					StatusEffect statusEffect = effectClass.getDeclaredConstructor().newInstance();
					statusEffect.readFrom(effectJson, savedGameStateHolder, relatedStores);
					byClassType.put(statusEffect.getClass(), statusEffect);
				} catch (ReflectiveOperationException e) {
					throw new InvalidSaveException("Failed to instantiate StatusEffect " + effectClass.getSimpleName() + ", expecting no-arg constructor\n" + e.getMessage());
				}
			}
		}
	}

	public int count() {
		return byClassType.size();
	}

	public Collection<StatusEffect> getAll() {
		return byClassType.values();
	}
}
