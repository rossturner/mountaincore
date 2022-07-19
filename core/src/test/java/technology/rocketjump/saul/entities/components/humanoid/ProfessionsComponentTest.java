package technology.rocketjump.saul.entities.components.humanoid;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.saul.jobs.model.Profession;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ProfessionsComponentTest {

	@Mock
	private Profession mockProfA;
	@Mock
	private Profession mockProfB;
	@Mock
	private Profession mockProfC;

	@Test
	public void hasAnyActiveProfession() {
		ProfessionsComponent professionsComponent = new ProfessionsComponent();
		professionsComponent.setSkillLevel(mockProfA, 50);
		professionsComponent.setSkillLevel(mockProfB, 30);

		assertThat(professionsComponent.hasAnyActiveProfession(Sets.newHashSet(mockProfA))).isTrue();
		assertThat(professionsComponent.hasAnyActiveProfession(Sets.newHashSet(mockProfC))).isFalse();
	}

	@Test
	public void addExperience_increasesLevel_whenNoSkillLevelExists() {
		ProfessionsComponent professionsComponent = new ProfessionsComponent();

		assertThat(professionsComponent.getSkillLevel(mockProfA)).isEqualTo(0);

		professionsComponent.experienceGained(1, mockProfA);

		assertThat(professionsComponent.getSkillLevel(mockProfA)).isEqualTo(1);

		professionsComponent.experienceGained(5, mockProfA);

		assertThat(professionsComponent.getSkillLevel(mockProfA)).isEqualTo(3);

	}

	@Test
	public void addExperience_limitsLevel_to100() {
		ProfessionsComponent professionsComponent = new ProfessionsComponent();

		assertThat(professionsComponent.getSkillLevel(mockProfA)).isEqualTo(0);

		professionsComponent.experienceGained(9999, mockProfA);

		assertThat(professionsComponent.getSkillLevel(mockProfA)).isEqualTo(100);

	}
}