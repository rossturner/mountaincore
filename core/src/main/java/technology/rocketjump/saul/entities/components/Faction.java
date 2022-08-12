package technology.rocketjump.saul.entities.components;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.rendering.utils.HexColors;

/**
 * This is used to determine ownership of items/furniture and who is on what side in combat
 */
public enum Faction {

	SETTLEMENT("#74d7e9", "#80ecff"),
	WILD_ANIMALS("#74e990", "#80ffaa"),
	MONSTERS("#e97474", "#ff8080"),
	HOSTILE_INVASION("#ffbb80", "#e9b274"),
	MERCHANTS("#e674e9", "#ff80fe");

	public final Color defensePoolShieldColor;
	public final Color defensePoolBarColor;

	Faction(String defensePoolShieldColor, String defensePoolBarColor) {
		this.defensePoolShieldColor = HexColors.get(defensePoolShieldColor);
		this.defensePoolBarColor = HexColors.get(defensePoolBarColor);
	}

}
