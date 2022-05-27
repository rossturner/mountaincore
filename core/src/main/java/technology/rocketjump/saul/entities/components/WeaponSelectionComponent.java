package technology.rocketjump.saul.entities.components;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.Optional;

public class WeaponSelectionComponent implements EntityComponent {

	private Optional<ItemType> selectedWeapon = Optional.empty();

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		WeaponSelectionComponent cloned = new WeaponSelectionComponent();
		cloned.selectedWeapon = this.selectedWeapon;
		return cloned;
	}

	public Optional<ItemType> getSelectedWeapon() {
		return selectedWeapon;
	}

	public void setSelectedWeapon(ItemType selectedWeaponType) {
		this.selectedWeapon = Optional.ofNullable(selectedWeaponType);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (selectedWeapon.isPresent()) {
			asJson.put("selected", selectedWeapon.get().getItemTypeName());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		String selectedItemTypeName = asJson.getString("selected");
		if (selectedItemTypeName != null) {
			selectedWeapon = Optional.ofNullable(relatedStores.itemTypeDictionary.getByName(selectedItemTypeName));
			if (selectedWeapon.isEmpty()) {
				throw new InvalidSaveException("Could not find weapon with item type name " + selectedItemTypeName);
			}
		}
	}
}
