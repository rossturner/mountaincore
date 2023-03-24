package technology.rocketjump.mountaincore.rendering.camera;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalSettingsTest {

	@Test
	public void checkDevModeDisabled() {
		assertThat(GlobalSettings.DEV_MODE).isFalse();
	}


}