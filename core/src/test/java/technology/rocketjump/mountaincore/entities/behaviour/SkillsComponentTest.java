package technology.rocketjump.mountaincore.entities.behaviour;

import org.junit.Before;
import org.junit.Test;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.jobs.model.SkillType;

import static org.fest.assertions.Assertions.assertThat;

public class SkillsComponentTest {

	private Skill profA;
	private Skill profB;
	private Skill profC;
	private Skill hauler;

	@Before
	public void setup() {
		profA = new Skill();
		profA.setName("profA");
		profA.setType(SkillType.PROFESSION);

		profB = new Skill();
		profB.setName("profB");
		profB.setType(SkillType.PROFESSION);

		profC = new Skill();
		profC.setName("profC");
		profC.setType(SkillType.PROFESSION);

		hauler = new Skill();
		hauler.setName("HAULER");
		hauler.setType(SkillType.PROFESSION);
	}

	@Test
	public void add() throws Exception {
		SkillsComponent component = new SkillsComponent();

		component.setSkillLevel(profB, 50);
		component.setSkillLevel(profA, 20);
		component.setSkillLevel(profC,80);

		assertThat(component.getActiveProfessions()).hasSize(4);
		assertThat(component.getActiveProfessions().get(0).getLevel()).isEqualTo(50);
		assertThat(component.getActiveProfessions().get(1).getLevel()).isEqualTo(20);
		assertThat(component.getActiveProfessions().get(2).getLevel()).isEqualTo(80);
		assertThat(component.getActiveProfessions().get(3).getLevel()).isEqualTo(50); // NULL_PROFESSION
	}

}