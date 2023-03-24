package technology.rocketjump.mountaincore.guice;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.name.Names;
import com.google.inject.spi.Message;
import technology.rocketjump.mountaincore.messaging.ThreadSafeMessageDispatcher;
import technology.rocketjump.mountaincore.sprites.DiffuseTerrainSpriteCacheProvider;
import technology.rocketjump.mountaincore.sprites.NormalTerrainSpriteCacheProvider;
import technology.rocketjump.mountaincore.sprites.TerrainSpriteCache;

import java.lang.reflect.Modifier;
import java.util.Arrays;

public class MountaincoreGuiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(TerrainSpriteCache.class).annotatedWith(Names.named("diffuse")).toProvider(DiffuseTerrainSpriteCacheProvider.class);
		bind(TerrainSpriteCache.class).annotatedWith(Names.named("normal")).toProvider(NormalTerrainSpriteCacheProvider.class);

		bind(MessageDispatcher.class).to(ThreadSafeMessageDispatcher.class).asEagerSingleton();
	}

	public static void checkForSingleton(Class aClass) {
		if (!aClass.isInterface() && !Modifier.isAbstract(aClass.getModifiers()) && !(
				aClass.isAnnotationPresent(javax.inject.Singleton.class) || aClass.isAnnotationPresent(com.google.inject.Singleton.class)
		)) {
			throw new ConfigurationException(Arrays.asList(new Message(aClass.getName() + " must be annotated with Singleton")));
		}
	}

}
