package technology.rocketjump.mountaincore.misc.twitch;

import org.junit.Test;
import technology.rocketjump.mountaincore.misc.twitch.model.TwitchViewer;

import static org.fest.assertions.Assertions.assertThat;


public class TwitchViewerTest {

	@Test
	public void generatesExpectedOutput() {
		assertThat(new TwitchViewer("dave").toName().toString()).isEqualTo("Dave");
		assertThat(new TwitchViewer("absalon___").toName().toString()).isEqualTo("Absalon");
		assertThat(new TwitchViewer("cat__bus").toName().toString()).isEqualTo("Cat Bus");
		assertThat(new TwitchViewer("baron_fou").toName().toString()).isEqualTo("Baron Fou");
	}

}