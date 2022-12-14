package technology.rocketjump.saul.entities.components;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.rendering.utils.HexColors;

/**
 * This is used to determine ownership of items/furniture and who is on what side in combat
 */
public enum Faction {

	SETTLEMENT("#74d7e9", "#80ecff"),
	WILD_ANIMALS("#c2c285", "#cfcf5a"),
	MONSTERS("#857ab8", "#6549e4"),
	HOSTILE_INVASION("#e69e66", "#f0c566"),
	MERCHANTS("#ffcbd7", "#b87a99");

	public final Color defensePoolShieldColor;
	public final Color defensePoolBarColor;
	public final String i18nKey;

	Faction(String defensePoolShieldColor, String defensePoolBarColor) {
		this.defensePoolShieldColor = HexColors.get(defensePoolShieldColor);
		this.defensePoolBarColor = HexColors.get(defensePoolBarColor);
		this.i18nKey = "FACTION." + name();
	}

}
