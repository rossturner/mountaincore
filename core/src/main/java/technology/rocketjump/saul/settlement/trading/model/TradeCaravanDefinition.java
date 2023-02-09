package technology.rocketjump.saul.settlement.trading.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleType;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.misc.Name;

import java.util.ArrayList;
import java.util.List;

public class TradeCaravanDefinition {

	@Name
	private String name;
	private TradeCaravanVehiclesDescriptor vehicles;
	private TradeCaravanCreatureDescriptor traders;
	private TradeCaravanCreatureDescriptor guards;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TradeCaravanVehiclesDescriptor getVehicles() {
		return vehicles;
	}

	public void setVehicles(TradeCaravanVehiclesDescriptor vehicles) {
		this.vehicles = vehicles;
	}

	public TradeCaravanCreatureDescriptor getTraders() {
		return traders;
	}

	public void setTraders(TradeCaravanCreatureDescriptor traders) {
		this.traders = traders;
	}

	public TradeCaravanCreatureDescriptor getGuards() {
		return guards;
	}

	public void setGuards(TradeCaravanCreatureDescriptor guards) {
		this.guards = guards;
	}

	public static class TradeCaravanVehiclesDescriptor {
		private String vehicleTypeName;
		@JsonIgnore
		private VehicleType vehicleType;
		private String draughtAnimal;
		@JsonIgnore
		private Race draughtAnimalRace;
		private int minQuantity;
		private int maxQuantity;
		private int maxInventoryPerVehicle;
		private int maxValuePerVehicleInventory;

		public String getVehicleTypeName() {
			return vehicleTypeName;
		}

		public void setVehicleTypeName(String vehicleTypeName) {
			this.vehicleTypeName = vehicleTypeName;
		}

		public VehicleType getVehicleType() {
			return vehicleType;
		}

		public void setVehicleType(VehicleType vehicleType) {
			this.vehicleType = vehicleType;
		}

		public String getDraughtAnimal() {
			return draughtAnimal;
		}

		public void setDraughtAnimal(String draughtAnimal) {
			this.draughtAnimal = draughtAnimal;
		}

		public Race getDraughtAnimalRace() {
			return draughtAnimalRace;
		}

		public void setDraughtAnimalRace(Race draughtAnimalRace) {
			this.draughtAnimalRace = draughtAnimalRace;
		}

		public int getMinQuantity() {
			return minQuantity;
		}

		public void setMinQuantity(int minQuantity) {
			this.minQuantity = minQuantity;
		}

		public int getMaxQuantity() {
			return maxQuantity;
		}

		public void setMaxQuantity(int maxQuantity) {
			this.maxQuantity = maxQuantity;
		}

		public int getMaxInventoryPerVehicle() {
			return maxInventoryPerVehicle;
		}

		public void setMaxInventoryPerVehicle(int maxInventoryPerVehicle) {
			this.maxInventoryPerVehicle = maxInventoryPerVehicle;
		}

		public int getMaxValuePerVehicleInventory() {
			return maxValuePerVehicleInventory;
		}

		public void setMaxValuePerVehicleInventory(int maxValuePerVehicleInventory) {
			this.maxValuePerVehicleInventory = maxValuePerVehicleInventory;
		}
	}

	public static class TradeCaravanCreatureDescriptor {

		private String raceName;
		@JsonIgnore
		private Race race;
		private String professionName;
		@JsonIgnore
		private Skill profession;
		private int minQuantityPerVehicle;
		private int maxQuantityPerVehicle;
		private List<QuantifiedItemTypeWithMaterial> inventoryItems = new ArrayList<>();
    	private int minWeaponSkill = 30;
		private int maxWeaponSkill = 50;
		private List<String> weaponItemTypes = new ArrayList<>();
		private List<String> shieldItemTypes = new ArrayList<>();
		private List<String> armorItemTypes = new ArrayList<>();

		public String getRaceName() {
			return raceName;
		}

		public void setRaceName(String raceName) {
			this.raceName = raceName;
		}

		public Race getRace() {
			return race;
		}

		public void setRace(Race race) {
			this.race = race;
		}

		public String getProfessionName() {
			return professionName;
		}

		public void setProfessionName(String professionName) {
			this.professionName = professionName;
		}

		public Skill getProfession() {
			return profession;
		}

		public void setProfession(Skill profession) {
			this.profession = profession;
		}

		public int getMinQuantityPerVehicle() {
			return minQuantityPerVehicle;
		}

		public void setMinQuantityPerVehicle(int minQuantityPerVehicle) {
			this.minQuantityPerVehicle = minQuantityPerVehicle;
		}

		public int getMaxQuantityPerVehicle() {
			return maxQuantityPerVehicle;
		}

		public void setMaxQuantityPerVehicle(int maxQuantityPerVehicle) {
			this.maxQuantityPerVehicle = maxQuantityPerVehicle;
		}

		public List<QuantifiedItemTypeWithMaterial> getInventoryItems() {
			return inventoryItems;
		}

		public void setInventoryItems(List<QuantifiedItemTypeWithMaterial> inventoryItems) {
			this.inventoryItems = inventoryItems;
		}

		public int getMinWeaponSkill() {
			return minWeaponSkill;
		}

		public void setMinWeaponSkill(int minWeaponSkill) {
			this.minWeaponSkill = minWeaponSkill;
		}

		public int getMaxWeaponSkill() {
			return maxWeaponSkill;
		}

		public void setMaxWeaponSkill(int maxWeaponSkill) {
			this.maxWeaponSkill = maxWeaponSkill;
		}

		public List<String> getWeaponItemTypes() {
			return weaponItemTypes;
		}

		public void setWeaponItemTypes(List<String> weaponItemTypes) {
			this.weaponItemTypes = weaponItemTypes;
		}

		public List<String> getShieldItemTypes() {
			return shieldItemTypes;
		}

		public void setShieldItemTypes(List<String> shieldItemTypes) {
			this.shieldItemTypes = shieldItemTypes;
		}

		public List<String> getArmorItemTypes() {
			return armorItemTypes;
		}

		public void setArmorItemTypes(List<String> armorItemTypes) {
			this.armorItemTypes = armorItemTypes;
		}
	}

}


