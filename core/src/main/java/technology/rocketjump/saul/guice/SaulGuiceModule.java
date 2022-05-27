package technology.rocketjump.saul.guice;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import technology.rocketjump.saul.messaging.ThreadSafeMessageDispatcher;
import technology.rocketjump.saul.sprites.DiffuseTerrainSpriteCacheProvider;
import technology.rocketjump.saul.sprites.NormalTerrainSpriteCacheProvider;
import technology.rocketjump.saul.sprites.TerrainSpriteCache;

public class SaulGuiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(TerrainSpriteCache.class).annotatedWith(Names.named("diffuse")).toProvider(DiffuseTerrainSpriteCacheProvider.class);
		bind(TerrainSpriteCache.class).annotatedWith(Names.named("normal")).toProvider(NormalTerrainSpriteCacheProvider.class);

		bind(MessageDispatcher.class).to(ThreadSafeMessageDispatcher.class).asEagerSingleton();
	}

}
