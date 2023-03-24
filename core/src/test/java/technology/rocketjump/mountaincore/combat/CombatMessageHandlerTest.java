package technology.rocketjump.mountaincore.combat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import technology.rocketjump.mountaincore.combat.model.WeaponAttack;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.combat.CombatDamageType;
import technology.rocketjump.mountaincore.entities.model.physical.combat.DefenseInfo;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.creature.features.RaceFeatures;
import technology.rocketjump.mountaincore.messaging.types.CombatAttackMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class CombatMessageHandlerTest {
    @Test
    void adjustDamageAmount_GivenNoSpecificDefense_ReturnsSameAmount() {
        int currentDamage = 5;
        WeaponAttack weaponAttack = new WeaponAttack();
        DefenseInfo defenseInfo = new DefenseInfo();
        Entity defender = stubEntity(defenseInfo);

        CombatAttackMessage message = new CombatAttackMessage(null, defender, weaponAttack, null);

        int adjustedDamage = CombatMessageHandler.adjustDamageAmount(currentDamage, message);

        assertThat(adjustedDamage).isEqualTo(5);
    }

    @Test
    void adjustDamageAmount_GivenRaceDefenceForSameAttack_ReducesDamageAmount() {
        int currentDamage = 5;
        WeaponAttack weaponAttack = new WeaponAttack();
        weaponAttack.setDamageType(CombatDamageType.STABBING);
        DefenseInfo defenseInfo = new DefenseInfo();
        defenseInfo.getDamageReduction().put(CombatDamageType.STABBING, 1);
        Entity defender = stubEntity(defenseInfo);

        CombatAttackMessage message = new CombatAttackMessage(null, defender, weaponAttack, null);

        int adjustedDamage = CombatMessageHandler.adjustDamageAmount(currentDamage, message);

        assertThat(adjustedDamage).isEqualTo(4);
    }

    @Test
    void adjustDamageAmount_GivenRaceHasWeaknessForGivenAttack_IncreasesDamageAmount() {
        int currentDamage = 5;
        WeaponAttack weaponAttack = new WeaponAttack();
        weaponAttack.setDamageType(CombatDamageType.CRUSHING);
        DefenseInfo defenseInfo = new DefenseInfo();
        defenseInfo.getDamageReduction().put(CombatDamageType.CRUSHING, -1);
        Entity defender = stubEntity(defenseInfo);

        CombatAttackMessage message = new CombatAttackMessage(null, defender, weaponAttack, null);

        int adjustedDamage = CombatMessageHandler.adjustDamageAmount(currentDamage, message);

        assertThat(adjustedDamage).isEqualTo(6);
    }

    @Test
    void adjustDamageAmount_GivenRaceDefenceForSameAttackAndArmorNegation_ReducesDamageAmount() {
        int currentDamage = 5;
        WeaponAttack weaponAttack = new WeaponAttack();
        weaponAttack.setDamageType(CombatDamageType.STABBING);
        weaponAttack.setArmorNegation(2);
        DefenseInfo defenseInfo = new DefenseInfo();
        defenseInfo.getDamageReduction().put(CombatDamageType.STABBING, 1);
        Entity defender = stubEntity(defenseInfo);

        CombatAttackMessage message = new CombatAttackMessage(null, defender, weaponAttack, null);

        int adjustedDamage = CombatMessageHandler.adjustDamageAmount(currentDamage, message);

        assertThat(adjustedDamage).isEqualTo(5);
    }

    @Test
    void adjustDamageAmount_GivenRaceHasWeaknessForGivenAttackButArmorNegationIsGreater_ReturnsSameAmount() {
        int currentDamage = 5;
        WeaponAttack weaponAttack = new WeaponAttack();
        weaponAttack.setDamageType(CombatDamageType.CRUSHING);
        weaponAttack.setArmorNegation(2);
        DefenseInfo defenseInfo = new DefenseInfo();
        defenseInfo.getDamageReduction().put(CombatDamageType.CRUSHING, -1);
        Entity defender = stubEntity(defenseInfo);

        CombatAttackMessage message = new CombatAttackMessage(null, defender, weaponAttack, null);

        int adjustedDamage = CombatMessageHandler.adjustDamageAmount(currentDamage, message);

        assertThat(adjustedDamage).isEqualTo(5);
    }

    @Test
    void adjustDamageAmount_GivenRaceHasWeaknessForGivenAttackAndArmorNegationIsSmaller_IncreasesDamageAmount() {
        int currentDamage = 5;
        WeaponAttack weaponAttack = new WeaponAttack();
        weaponAttack.setDamageType(CombatDamageType.CRUSHING);
        weaponAttack.setArmorNegation(2);
        DefenseInfo defenseInfo = new DefenseInfo();
        defenseInfo.getDamageReduction().put(CombatDamageType.CRUSHING, -3);
        Entity defender = stubEntity(defenseInfo);

        CombatAttackMessage message = new CombatAttackMessage(null, defender, weaponAttack, null);

        int adjustedDamage = CombatMessageHandler.adjustDamageAmount(currentDamage, message);

        assertThat(adjustedDamage).isEqualTo(6);
    }


    private Entity stubEntity(DefenseInfo defenseInfo) {
        RaceFeatures raceFeatures = new RaceFeatures();
        raceFeatures.setDefense(defenseInfo);
        Race race = Mockito.mock(Race.class);
        CreatureEntityAttributes creatureAttributes = Mockito.mock(CreatureEntityAttributes.class);
        PhysicalEntityComponent physicalComponent = Mockito.mock(PhysicalEntityComponent.class);
        Entity defender = Mockito.mock(Entity.class);
        when(defender.getType()).thenReturn(EntityType.CREATURE);
        when(defender.getPhysicalEntityComponent()).thenReturn(physicalComponent);
        when(physicalComponent.getAttributes()).thenReturn(creatureAttributes);
        when(creatureAttributes.getRace()).thenReturn(race);
        when(race.getFeatures()).thenReturn(raceFeatures);
        return defender;
    }
}
