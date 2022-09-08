package technology.rocketjump.saul.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import technology.rocketjump.saul.rendering.custom_libgdx.ShaderLoader;

public class OutlineExtensionRenderer implements Disposable {

    private static final int NUM_TRIANGLES = 2;
    private static final int VERTEX_SIZE = 2;
    private static final int NUM_INDEX_PER_TRIANGLE = 3;
    //Remember these are column oriented

    private FrameBuffer[] frameBuffers;
    private TextureRegion[] textureRegions;
    private int currentFrameBufferIndex;
    private final ShaderProgram shader;
    private final Mesh fullScreenMesh;
    private int width;
    private int height;

    public OutlineExtensionRenderer() {
        FileHandle vertexShader = Gdx.files.classpath("shaders/combined_lighting_vertex_shader.glsl");
        FileHandle fragmentShader = Gdx.files.classpath("shaders/outline_extension_fragment_shader.glsl");
        shader = ShaderLoader.createShader(vertexShader, fragmentShader);

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
        FrameBuffer frameBuffer = frameBuffers[currentFrameBufferIndex];
        TextureRegion textureRegion = textureRegions[currentFrameBufferIndex];
        currentFrameBufferIndex++;
        if (currentFrameBufferIndex >= frameBuffers.length) {
            currentFrameBufferIndex = 0;
        }


        frameBuffer.begin();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        shader.begin();
        shader.setUniformi("u_texture", 0);
        input.getTexture().bind(0);
//        shader.setUniform2f("u_step", step);
//        shader.setUniformi("u_reduceByMax", reduceByMax ? 1 : 0);
        shader.setUniform2fv("u_viewportResolution", new float[] {width, height}, 0, 2);
//        shader.setUniformMatrix("u_kernelX", kernelX);
//        shader.setUniformMatrix("u_kernelY", kernelY);

        fullScreenMesh.render(shader, GL20.GL_TRIANGLES);

        shader.end();
        frameBuffer.end();

        return textureRegion;
    }

    @Override
    public void dispose() {
        for (int i = 0; i < frameBuffers.length; i++) {
            this.frameBuffers[i].dispose();
        }
    }

    public void initFrameBuffers(int widthInput, int heightInput) {
        this.width = widthInput/2;
        this.height = heightInput/2;

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
