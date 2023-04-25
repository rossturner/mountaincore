package technology.rocketjump.mountaincore.ui.skins;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.FactionComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;

public class MainGameSkin extends Skin {
	public static final String MISERABLE = "icon_happiness_and_injury_happiness_0";
	public static final String SAD = "icon_happiness_and_injury_happiness_1";
	public static final String DOWN = "icon_happiness_and_injury_happiness_2";
	public static final String NEUTRAL = "icon_happiness_and_injury_happiness_3";
	public static final String CHEERY = "icon_happiness_and_injury_happiness_4";
	public static final String JOLLY = "icon_happiness_and_injury_happiness_5";
	public static final String HAPPY = "icon_happiness_and_injury_happiness_6";
	public static final String ECSTATIC = "icon_happiness_and_injury_happiness_7";
	public static final String INJURED = "icon_happiness_and_injury_injured";
	public static final String NOT_INJURED = "icon_happiness_and_injury_not_injured";
	public static final String ANIMAL_INJURED = "icon_happiness_and_injury_animal_injured";
	public static final String ANIMAL_NOT_INJURED = "icon_happiness_and_injury_animal_not_injured";
	public static final String INVADER_INJURED = "icon_happiness_and_injury_invader_injured";
	public static final String INVADER_NOT_INJURED = "icon_happiness_and_injury_invader_not_injured";
	public static final String MERCHANT_INJURED = "icon_happiness_and_injury_merchant_injured";
	public static final String MERCHANT_NOT_INJURED = "icon_happiness_and_injury_merchant_not_injured";
	public static final String MONSTER_INJURED = "icon_happiness_and_injury_monster_injured";
	public static final String MONSTER_NOT_INJURED = "icon_happiness_and_injury_monster_not_injured";


	public Drawable getInjuredSmiley(CreatureEntityAttributes attributes, FactionComponent factionComponent) {
		String drawableName = INJURED;
		if (factionComponent != null) {
			Faction faction = factionComponent.getFaction();
			drawableName = switch (faction) {
				case SETTLEMENT -> attributes.getRace().getBehaviour().getIsSapient() ? INJURED : ANIMAL_INJURED;
				case PIRATES -> INVADER_INJURED;
				case WILD_ANIMALS -> ANIMAL_INJURED;
				case MONSTERS -> MONSTER_INJURED;
				case HOSTILE_INVASION -> INVADER_INJURED;
				case MERCHANTS -> MERCHANT_INJURED;
			};
		}
		return getDrawable(drawableName);
	}

	public Drawable getNotInjuredSmiley(CreatureEntityAttributes attributes, FactionComponent factionComponent) {
		String drawableName = NOT_INJURED;
		if (factionComponent != null) {
			Faction faction = factionComponent.getFaction();
			drawableName = switch (faction) {
				case SETTLEMENT -> attributes.getRace().getBehaviour().getIsSapient() ? NOT_INJURED : ANIMAL_NOT_INJURED;
				case PIRATES -> INVADER_NOT_INJURED;
				case WILD_ANIMALS -> ANIMAL_NOT_INJURED;
				case MONSTERS -> MONSTER_NOT_INJURED;
				case HOSTILE_INVASION -> INVADER_NOT_INJURED;
				case MERCHANTS -> MERCHANT_NOT_INJURED;
			};
		}
		return getDrawable(drawableName);
	}
}
