package technology.rocketjump.saul.assets.editor;


import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.FileUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

@Singleton
public class SpriteCropperPipeline {

    private final SpriteCropper spriteCropper;
    private final NormalMapGenerator normalMapGenerator;
    private final MessageDispatcher messageDispatcher;

    @Inject
    public SpriteCropperPipeline(SpriteCropper spriteCropper, NormalMapGenerator normalMapGenerator, MessageDispatcher messageDispatcher) {
        this.spriteCropper = spriteCropper;
        this.normalMapGenerator = normalMapGenerator;
        this.messageDispatcher = messageDispatcher;
    }

    //TODO: can be much better, reduce duplication
    public void process(Path pathToTraverse) {
        try {
            spriteCropper.processDirectory(pathToTraverse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<Path> normalImages = FileUtils.findFilesByFilename(pathToTraverse, Pattern.compile(".*_NORMALS\\.png"));
        List<Path> diffuseImages = FileUtils.findFilesByFilename(pathToTraverse, Pattern.compile(".*\\.png"))
                .stream()
                .filter(path -> !normalImages.contains(path))
                .filter(path -> !path.getFileName().toString().contains("-swatch.png"))
                .toList();

        normalImages.forEach(FileUtils::delete);
        diffuseImages.forEach(normalMapGenerator::generate);

        messageDispatcher.dispatchMessage(MessageType.EDITOR_RELOAD, null);
    }


}
