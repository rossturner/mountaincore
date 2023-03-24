package technology.rocketjump.mountaincore.screens.menus;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ResolutionTest {

	@Test
	public void byString() {
		Resolution resolution = Resolution.byString("800x800");

		assertThat(resolution.toString()).isEqualTo("800x800");
	}

	@Test
	public void byString_throwsException_withBadInput() {
		Assertions.assertThatThrownBy(() -> {
			Resolution.byString("abc");
		}).isInstanceOf(NumberFormatException.class);
	}
}