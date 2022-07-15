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
}