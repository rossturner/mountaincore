package technology.rocketjump.saul.entities.factories;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.misc.twitch.TwitchDataStore;
import technology.rocketjump.saul.misc.twitch.model.TwitchViewer;
import technology.rocketjump.saul.persistence.UserPreferences;

@Singleton
public class SettlerCreatureAttributesFactory {

	private final DwarvenNameGenerator nameGenerator;
	private final UserPreferences userPreferences;
	private final TwitchDataStore twitchDataStore;
	private final Race dwarfRace;

	@Inject
	public SettlerCreatureAttributesFactory(DwarvenNameGenerator nameGenerator,
											UserPreferences userPreferences, TwitchDataStore twitchDataStore,
											RaceDictionary raceDictionary) {
		this.nameGenerator = nameGenerator;
		this.userPreferences = userPreferences;
		this.twitchDataStore = twitchDataStore;

		this.dwarfRace = raceDictionary.getByName("Dwarf");
	}

	public CreatureEntityAttributes create(GameContext gameContext) {
		CreatureEntityAttributes attributes = new CreatureEntityAttributes(dwarfRace, gameContext.getRandom().nextLong());

		if (twitchSettlerNameReplacementsEnabled()) {
			for (TwitchViewer twitchViewer : twitchDataStore.getPrioritisedViewers()) {
				if (!gameContext.getSettlementState().usedTwitchViewers.contains(twitchViewer)) {
					attributes.setName(twitchViewer.toName());
					gameContext.getSettlementState().usedTwitchViewers.add(twitchViewer);
					break;
				}
			}
		}

		if (attributes.getName() == null) {
			attributes.setName(nameGenerator.create(attributes.getSeed(), attributes.getGender()));
		}

		return attributes;
	}

	private boolean twitchSettlerNameReplacementsEnabled() {
		return Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.TWITCH_INTEGRATION_ENABLED, "false")) &&
				Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.TWITCH_VIEWERS_AS_SETTLER_NAMES, "false"));
	}



}
