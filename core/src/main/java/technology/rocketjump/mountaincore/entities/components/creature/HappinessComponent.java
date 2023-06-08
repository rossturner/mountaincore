package technology.rocketjump.mountaincore.entities.components.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.mountaincore.entities.components.EntityComponent;
import technology.rocketjump.mountaincore.entities.components.InfrequentlyUpdatableComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.*;

/**
 * This class is to keep track of current changes in a Settler's happiness
 */
public class HappinessComponent implements InfrequentlyUpdatableComponent {

	public static final int MAX_HAPPINESS_VALUE = 100;
	public static final int MIN_HAPPINESS_VALUE = -100;
	private final Map<HappinessModifier, Double> timesToExpiry = new EnumMap<>(HappinessModifier.class);

	private int netModifier = 0;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {

	}

	@Override
	public void infrequentUpdate(double elapsedTime) {
		for (HappinessModifier happinessModifier : new HashSet<>(timesToExpiry.keySet())) {
			double currentExpiry = timesToExpiry.get(happinessModifier);
			double newExpiry = currentExpiry - elapsedTime;
			if (newExpiry < 0) {
				timesToExpiry.remove(happinessModifier);
			} else {
				timesToExpiry.put(happinessModifier, newExpiry);
			}
		}

		updateNetModifier();
	}

	public Set<HappinessModifier> currentModifiers() {
		return timesToExpiry.keySet();
	}

	/**
	 * Re-adding a HappinessModifier will reset the time to expiry
	 */
	public void add(HappinessModifier happinessModifier) {
		for (HappinessModifier existingModifier : currentModifiers()) {
			if (existingModifier.replaces.contains(happinessModifier)) {
				return;
			}
			if (existingModifier.replacedBy.contains(happinessModifier)) {
				timesToExpiry.remove(existingModifier);
			}
		}

		this.timesToExpiry.put(happinessModifier, happinessModifier.hoursToExpiry);

		updateNetModifier();
	}

	public int getNetModifier() {
		return netModifier;
	}

	private void updateNetModifier() {
		int updatedModifier = 0;
		for (HappinessModifier modifier : timesToExpiry.keySet()) {
			updatedModifier += modifier.modifierAmount;
		}
		updatedModifier = Math.min(MAX_HAPPINESS_VALUE, updatedModifier);
		updatedModifier = Math.max(MIN_HAPPINESS_VALUE, updatedModifier);

		this.netModifier = updatedModifier;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		HappinessComponent clone = new HappinessComponent();
		clone.timesToExpiry.putAll(timesToExpiry);
		return clone;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		JSONObject timeToExpiryJson = new JSONObject(true);
		for (Map.Entry<HappinessModifier, Double> entry : timesToExpiry.entrySet()) {
			timeToExpiryJson.put(entry.getKey().name(), entry.getValue());
		}
		asJson.put("expiry", timeToExpiryJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONObject timeToExpiryJson = asJson.getJSONObject("expiry");
		if (timeToExpiryJson == null) {
			throw new InvalidSaveException("Could not find expiry JSON in " + this.getClass().getSimpleName());
		}
		for (String modifierName : timeToExpiryJson.keySet()) {
			if (!EnumUtils.isValidEnum(HappinessModifier.class, modifierName)) {
				throw new InvalidSaveException("Could not parse " + HappinessModifier.class.getSimpleName() + " with name " + modifierName);
			}
			HappinessModifier modifier = HappinessModifier.valueOf(modifierName);
			Double expiryTime = timeToExpiryJson.getDoubleValue(modifierName);
			timesToExpiry.put(modifier, expiryTime);
		}

		updateNetModifier();
	}

	/**
	 * These probably want to be data-driven by race, so dwarves hate sleeping outside but elves do not
	 *
	 * Want to ensure that every instance is used somewhere
	 */
	public enum HappinessModifier {

		NEW_SETTLEMENT_OPTIMISM(40, 24.0 * 15),
		WORKED_IN_ENCLOSED_ROOM(20, 16.0),

		SAW_DEAD_BODY(-40, 24.0),
		CARRIED_DEAD_BODY(-45, 24.0),

		ON_FIRE(-40, 16.0),

		DRANK_FROM_RIVER(-5, 1.5),
		ATE_NICELY_PREPARED_FOOD(5, 3.0),

		VERY_TIRED(-25, 2.5),
		POISONED(-30, 2.5),
		VERY_HUNGRY(-30, 0.5),
		VERY_THIRSTY(-30, 0.5),
		DYING_OF_HUNGER(-50, 0.5),
		DYING_OF_THIRST(-50, 0.5),

		SLEPT_OUTSIDE(-20, 3.0),
		SLEPT_ON_GROUND(-10, 5.0),
		SLEPT_IN_BED(20, 5.0),

		CAUGHT_IN_RAIN(-10, 0.5),
		WORKED_IN_RAIN(-20, 0.5),
		SLEEPING_IN_RAIN(-40, 4),
		SLEEPING_IN_SNOW(-40, 4),

		DRANK_ALCOHOL(40, 8),
		ALCOHOL_WITHDRAWAL(-20, 0.5),

		HAD_A_TANTRUM(100, 24.0),
		CAUSE_BREAKDOWN(-180, 1.0),

		BLEEDING(-5, 0.3);

		public final int modifierAmount;
		private final double hoursToExpiry;
		private final List<HappinessModifier> replaces = new ArrayList<>();
		private final List<HappinessModifier> replacedBy = new ArrayList<>();

		static {
			CARRIED_DEAD_BODY.replaces.add(SAW_DEAD_BODY);

			DYING_OF_HUNGER.replaces.add(VERY_HUNGRY);
			DYING_OF_THIRST.replaces.add(VERY_THIRSTY);

			DRANK_ALCOHOL.replaces.add(ALCOHOL_WITHDRAWAL);

			SLEEPING_IN_RAIN.replaces.add(WORKED_IN_RAIN);
			SLEEPING_IN_RAIN.replaces.add(CAUGHT_IN_RAIN);
			SLEEPING_IN_RAIN.replaces.add(SLEPT_OUTSIDE);
			SLEEPING_IN_RAIN.replaces.add(SLEEPING_IN_SNOW);

			WORKED_IN_RAIN.replaces.add(CAUGHT_IN_RAIN);

			SLEEPING_IN_SNOW.replaces.add(SLEEPING_IN_RAIN); // Just one of sleeping in rain or snow appliesa

			for (HappinessModifier happinessModifier : HappinessModifier.values()) {
				for (HappinessModifier otherModifier : HappinessModifier.values()) {
					if (happinessModifier.equals(otherModifier)) {
						continue;
					}
					if (otherModifier.replaces.contains(happinessModifier)) {
						happinessModifier.replacedBy.add(otherModifier);
					}
				}
			}
		}


		HappinessModifier(int modifierAmount, double hoursToExpiry) {
			this.modifierAmount = modifierAmount;
			this.hoursToExpiry = hoursToExpiry;
		}

		public String getI18nKey() {
			return "HAPPINESS_MODIFIER."+name();
		}
	}
}
