package technology.rocketjump.mountaincore.misc;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.gamecontext.GameContextRegister;
import technology.rocketjump.mountaincore.modding.LocalModRepository;
import technology.rocketjump.mountaincore.persistence.FileUtils;

import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Singleton
public class ReflectionsService {
    private final Reflections reflections;
    private final Injector injector;
    private final GameContextRegister gameContextRegister;

    @Inject
    public ReflectionsService(Injector injector, GameContextRegister gameContextRegister) {
        this.injector = injector;
        this.gameContextRegister = gameContextRegister;
        Path assetsCodeDir = LocalModRepository.ASSETS_DIR.resolve("code");
        if (assetsCodeDir.toFile().exists()) {
            List<Path> jarFiles = FileUtils.findFilesByFilename(assetsCodeDir, Pattern.compile(".*\\.jar"));
            URL[] urls = jarFiles.stream()
                    .map(Path::toUri)
                    .map(uri -> {
                        try {
                            return uri.toURL();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toArray(URL[]::new);
            URLClassLoader modClassLoader = new URLClassLoader("ModClassLoader", urls, LocalModRepository.class.getClassLoader());
            this.reflections = new Reflections(ClasspathHelper.forPackage("technology.rocketjump"), ClasspathHelper.forClassLoader(modClassLoader), modClassLoader); //man this was hard to figure
        } else {
            this.reflections = new Reflections(ClasspathHelper.forPackage("technology.rocketjump"));
        }
    }

    public <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type) {
        return reflections.getSubTypesOf(type);
    }

    public <T> T getInjectedInstance(Class<T> type) {
        T instance = injector.getInstance(type);
        if (GameContextAware.class.isAssignableFrom(type) ) {
            Class<? extends GameContextAware> gcaType = (Class<? extends GameContextAware>) type;
            if (!gameContextRegister.isRegistered(gcaType)) {
                gameContextRegister.register((GameContextAware) instance);
            }
        }
        return instance;
    }
}
