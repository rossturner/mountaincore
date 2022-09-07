package technology.rocketjump.saul;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.google.inject.AbstractModule;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.persistence.UserFileManager;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.ui.i18n.I18nRepo;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TestModule extends AbstractModule {

	private final TextureAtlasRepository mockTextureAtlasRepository = mock(TextureAtlasRepository.class);
	private final TextureAtlas mockTextureAtlas = mock(TextureAtlas.class);
	private final EntityAssetUpdater mockEntityAssetUpdater = mock(EntityAssetUpdater.class);

	public TestModule() {
		when(mockTextureAtlasRepository.get(any())).thenReturn(mockTextureAtlas);

		Sprite sprite = new Sprite();
		when(mockTextureAtlas.createSprite(anyString())).thenReturn(sprite);
		Array<Sprite> spriteArray = new Array<>();
		spriteArray.add(sprite);
		when(mockTextureAtlas.createSprites(anyString())).thenReturn(spriteArray);
	}

	@Override
	protected void configure() {
		try {
			bind(I18nTranslator.class).toInstance(stubbedI18nTranslater());
			bind(TextureAtlasRepository.class).toInstance(mockTextureAtlasRepository);
			bind(EntityAssetUpdater.class).toInstance(mockEntityAssetUpdater);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private I18nTranslator stubbedI18nTranslater() throws IOException {
		return new I18nTranslator(
				new I18nRepo(new UserPreferences(new UserFileManager())),
				new SkillDictionary(),
				null
		);
	}

}
