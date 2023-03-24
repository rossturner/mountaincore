package technology.rocketjump.mountaincore.assets.editor;


import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NormalMapGeneratorTest {

    private final NormalMapGenerator normalMapGenerator = new NormalMapGenerator();

    @Test
    void generate_GivenValidImageInputFile_ReturnsGeneratedNormalFile() {
        Path inputImageFile = new File(this.getClass().getResource("/test-alpaca-image.png").getFile()).toPath();
        Path normalImageFile = normalMapGenerator.generate(inputImageFile);
        assertThat(normalImageFile).isNotEmptyFile();
    }
}