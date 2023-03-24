package technology.rocketjump.mountaincore;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class HeadlessGdxTest {

	private static HeadlessApplication application;

	@BeforeClass
	public static void beforeClass() {
		final HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
		application = new HeadlessApplication(new MountaincoreApplicationAdapter(), config);
	}

	@AfterClass
	public static void tearDown() {
		application.exit();
	}
}
