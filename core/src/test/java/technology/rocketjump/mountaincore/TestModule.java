package technology.rocketjump.mountaincore;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.google.inject.AbstractModule;
import technology.rocketjump.mountaincore.assets.TextureAtlasRepository;
import technology.rocketjump.mountaincore.entities.EntityAssetUpdater;
import technology.rocketjump.mountaincore.persistence.UserFileManager;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.ui.i18n.I18nRepo;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TestModule extends AbstractModule {

	private final TextureAtlasRepository mockTextureAtlasRepository = mock(TextureAtlasRepository.class);
	private final TextureAtlas mockTextureAtlas = mock(TextureAtlas.class);
	private final EntityAssetUpdater mockEntityAssetUpdater = mock(EntityAssetUpdater.class);

	public TestModule() {
		lenient().when(mockTextureAtlasRepository.get(any())).thenReturn(mockTextureAtlas);

		Sprite sprite = new Sprite();
		lenient().when(mockTextureAtlas.createSprite(anyString())).thenReturn(sprite);
		Array<Sprite> spriteArray = new Array<>();
		spriteArray.add(sprite);
		lenient().when(mockTextureAtlas.createSprites(anyString())).thenReturn(spriteArray);
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
				null
		);
	}

}
