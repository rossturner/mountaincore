package technology.rocketjump.saul.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.utils.Disposable;
import technology.rocketjump.saul.rendering.custom_libgdx.ShaderLoader;

public class ImageProcessingRenderer implements Disposable {

    private static final int NUM_TRIANGLES = 2;
    private static final int VERTEX_SIZE = 2;
    private static final int NUM_INDEX_PER_TRIANGLE = 3;
    private static final float ONE_NINTH = 1.0f;
    //Remember these are column oriented
    private static final Matrix3 ZEROS_KERNEL = new Matrix3(new float[] {
       0, 0, 0,
       0, 0, 0,
       0, 0, 0
    });

    private static final Matrix3 GAUSSIAN_BLUR_KERNEL = new Matrix3(new float[] {
            1.0f/16.0f, 1.0f/8.0f, 1.0f/16.0f,
            1.0f/8.0f, 1.0f/4.0f, 1.0f/8.0f,
            1.0f/16.0f, 1.0f/8.0f, 1.0f/16.0f,
    });

    private static final Matrix3 DILATE_KERNEL = new Matrix3(new float[] {
            ONE_NINTH, ONE_NINTH, ONE_NINTH,
            ONE_NINTH, ONE_NINTH, ONE_NINTH,
            ONE_NINTH, ONE_NINTH, ONE_NINTH
    });

    private static final Matrix3 SOBEL_KERNEL_X = new Matrix3(new float[] {
            1, 2, 1,
            0, 0, 0,
            -1, -2, -1
    });

    private static final Matrix3 SOBEL_KERNEL_Y = new Matrix3(new float[] {
            1, 0, -1,
            2, 0, -2,
            1, 0, -1
    });

    private FrameBuffer[] frameBuffers;
    private TextureRegion[] textureRegions;
    private int currentFrameBufferIndex;
    private final ShaderProgram imageProcessorShader;
    private final Mesh fullScreenMesh;
    private int width;
    private int height;

    public ImageProcessingRenderer() {
        new GLProfiler(Gdx.graphics).enable();

        FileHandle vertexShader = Gdx.files.classpath("shaders/combined_lighting_vertex_shader.glsl");
        FileHandle fragmentShader = Gdx.files.classpath("shaders/image_processing_fragment_shader.glsl");
        imageProcessorShader = ShaderLoader.createShader(vertexShader, fragmentShader);

        final int MAX_VERTICES = NUM_TRIANGLES * VERTEX_SIZE * 3;
        float[] vertices = new float[MAX_VERTICES];

        int MAX_INDICES = NUM_TRIANGLES * NUM_INDEX_PER_TRIANGLE;
        short[] indices = new short[MAX_INDICES];

        fullScreenMesh = new Mesh(Mesh.VertexDataType.VertexArray, true, MAX_VERTICES, MAX_INDICES,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE)
        );

        int vertexIndex = 0;

        vertices[vertexIndex++] = -1f;
        vertices[vertexIndex++] = -1f;

        vertices[vertexIndex++] = -1f;
        vertices[vertexIndex++] = 1f;

        vertices[vertexIndex++] = 1f;
        vertices[vertexIndex++] = 1f;

        vertices[vertexIndex++] = 1f;
        vertices[vertexIndex++] = -1f;
        fullScreenMesh.setVertices(vertices);

        indices[0] = 0; // indices for triangle with vertices [0, 1, 2] [2, 3, 0]
        indices[1] = 1;
        indices[2] = 2;
        indices[3] = 2;
        indices[4] = 3;
        indices[5] = 0;
        fullScreenMesh.setIndices(indices);
    }

    public TextureRegion outline(TextureRegion input) {
        return process(input, SOBEL_KERNEL_X, SOBEL_KERNEL_Y);
    }

    public TextureRegion dilate(TextureRegion input) {
        return process(input, DILATE_KERNEL, ZEROS_KERNEL);
    }

    public TextureRegion blur(TextureRegion input) {
        return process(input, GAUSSIAN_BLUR_KERNEL, ZEROS_KERNEL);
    }

    private TextureRegion process(TextureRegion input, Matrix3 kernelX, Matrix3 kernelY) {
        FrameBuffer frameBuffer = frameBuffers[currentFrameBufferIndex];
        TextureRegion textureRegion = textureRegions[currentFrameBufferIndex];
        currentFrameBufferIndex++;
        if (currentFrameBufferIndex >= frameBuffers.length) {
            currentFrameBufferIndex = 0;
        }


        frameBuffer.begin();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glClearColor(0.5f, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        imageProcessorShader.begin();
        imageProcessorShader.setUniformi("u_texture", 0);
        input.getTexture().bind(0);
        imageProcessorShader.setUniform2fv("u_viewportResolution", new float[] {width, height}, 0, 2);
        imageProcessorShader.setUniformMatrix("u_kernelX", kernelX);
        imageProcessorShader.setUniformMatrix("u_kernelY", kernelY);

        fullScreenMesh.render(imageProcessorShader, GL20.GL_TRIANGLES);

        imageProcessorShader.end();
        frameBuffer.end();

        return textureRegion;
    }

    @Override
    public void dispose() {
        for (int i = 0; i < frameBuffers.length; i++) {
            this.frameBuffers[i].dispose();
        }
    }

    public void initFrameBuffers(int width, int height) {
        this.width = width;
        this.height = height;

        FrameBuffer firstFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false, false);
        FrameBuffer secondFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false, false);
        frameBuffers = new FrameBuffer[] {firstFrameBuffer, secondFrameBuffer};
        currentFrameBufferIndex = 0;

        TextureRegion firstTextureRegion = new TextureRegion(firstFrameBuffer.getColorBufferTexture(), width, height);
        firstTextureRegion.flip(false, true);
        TextureRegion secondTextureRegion = new TextureRegion(secondFrameBuffer.getColorBufferTexture(), width, height);
        secondTextureRegion.flip(false, true);
        textureRegions = new TextureRegion[] {firstTextureRegion, secondTextureRegion};
    }

    public TextureRegion getFirstTextureRegion() {
        return textureRegions[0];
    }

    public TextureRegion getSecondTextureRegion() {
        return textureRegions[1];
    }
}
