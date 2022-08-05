package technology.rocketjump.saul.entities.components.creature;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.saul.jobs.model.Skill;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SkillsComponentTest {

	@Mock
	private Skill mockProfA;
	@Mock
	private Skill mockProfB;
	@Mock
	private Skill mockProfC;

	@Test
	public void hasAnyActiveProfession() {
		SkillsComponent skillsComponent = new SkillsComponent();
		skillsComponent.setSkillLevel(mockProfA, 50);
		skillsComponent.setSkillLevel(mockProfB, 30);

		assertThat(skillsComponent.hasAnyActiveProfession(Sets.newHashSet(mockProfA))).isTrue();
		assertThat(skillsComponent.hasAnyActiveProfession(Sets.newHashSet(mockProfC))).isFalse();
	}

	@Test
	public void addExperience_increasesLevel_whenNoSkillLevelExists() {
		SkillsComponent skillsComponent = new SkillsComponent();

		assertThat(skillsComponent.getSkillLevel(mockProfA)).isEqualTo(0);

		skillsComponent.experienceGained(1, mockProfA);

		assertThat(skillsComponent.getSkillLevel(mockProfA)).isEqualTo(0);

		skillsComponent.experienceGained(5, mockProfA);

		assertThat(skillsComponent.getSkillLevel(mockProfA)).isEqualTo(1);

		skillsComponent.experienceGained(20, mockProfA);

		assertThat(skillsComponent.getSkillLevel(mockProfA)).isEqualTo(5);

	}

	@Test
	public void addExperience_limitsLevel_to100() {
		SkillsComponent skillsComponent = new SkillsComponent();

		assertThat(skillsComponent.getSkillLevel(mockProfA)).isEqualTo(0);

		skillsComponent.experienceGained(9999, mockProfA);

		assertThat(skillsComponent.getSkillLevel(mockProfA)).isEqualTo(100);

	}
}