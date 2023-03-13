package technology.rocketjump.saul.misc;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import technology.rocketjump.saul.modding.LocalModRepository;
import technology.rocketjump.saul.persistence.FileUtils;

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
    private final URLClassLoader modClassLoader;
    private final Reflections reflections;
    public ReflectionsService() {
        Path assetsCodeDir = LocalModRepository.ASSETS_DIR.resolve("code");
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
        this.modClassLoader = new URLClassLoader("ModClassLoader", urls, LocalModRepository.class.getClassLoader());
        this.reflections = new Reflections(ClasspathHelper.forPackage("technology.rocketjump"), ClasspathHelper.forClassLoader(modClassLoader), modClassLoader); //man this was hard to figure
    }

    public <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type) {
        return reflections.getSubTypesOf(type);
    }
}
