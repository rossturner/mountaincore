package technology.rocketjump.saul;

import com.google.inject.AbstractModule;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.persistence.UserFileManager;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.ui.i18n.I18nRepo;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.io.IOException;

public class TestModule extends AbstractModule {

	@Override
	protected void configure() {
		try {
			bind(I18nTranslator.class).toInstance(stubbedI18nTranslater());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private I18nTranslator stubbedI18nTranslater() throws IOException {
		return new I18nTranslator(
				new I18nRepo(new UserPreferences(new UserFileManager())),
				new ProfessionDictionary(),
				null
		);
	}

}
