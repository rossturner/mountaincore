package technology.rocketjump.saul.entities.behaviour;

import org.junit.Before;
import org.junit.Test;
import technology.rocketjump.saul.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.saul.jobs.model.Profession;

import static org.fest.assertions.Assertions.assertThat;

public class ProfessionsComponentTest {

	private Profession profA;
	private Profession profB;
	private Profession profC;
	private Profession villager;

	@Before
	public void setup() {
		profA = new Profession();
		profA.setName("profA");

		profB = new Profession();
		profB.setName("profB");

		profC = new Profession();
		profC.setName("profC");

		villager = new Profession();
		villager.setName("VILLAGER");
	}

	@Test
	public void add() throws Exception {
		ProfessionsComponent component = new ProfessionsComponent();

		component.setSkillLevel(profB, 0.5f);
		component.setSkillLevel(profA, 0.2f);
		component.setSkillLevel(profC,0.8f);

		assertThat(component.getActiveProfessions()).hasSize(4);
		assertThat(component.getActiveProfessions().get(0).getSkillLevel()).isEqualTo(0.8f);
		assertThat(component.getActiveProfessions().get(1).getSkillLevel()).isEqualTo(0.5f);
		assertThat(component.getActiveProfessions().get(2).getSkillLevel()).isEqualTo(0.2f);
		assertThat(component.getActiveProfessions().get(3).getSkillLevel()).isEqualTo(0f);
	}

}