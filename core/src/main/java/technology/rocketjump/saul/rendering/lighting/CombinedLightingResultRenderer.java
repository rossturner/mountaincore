package technology.rocketjump.saul.rendering.lighting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import technology.rocketjump.saul.rendering.custom_libgdx.ShaderLoader;

public class CombinedLightingResultRenderer implements Disposable {

	private static final int NUM_TRIANGLES = 2;
	private static final int VERTEX_SIZE = 2;
	private static final int NUM_INDEX_PER_TRIANGLE = 3;
	private final ShaderProgram combinedShader;
	private final ShaderProgram entityOutlineShader;
	private final ShaderProgram dilateImageShader;
	private Mesh fullScrenMesh;

	@Inject
	public CombinedLightingResultRenderer() {
		final int MAX_VERTICES = NUM_TRIANGLES * VERTEX_SIZE * 3;
		float[] vertices = new float[MAX_VERTICES];

		int MAX_INDICES = NUM_TRIANGLES * NUM_INDEX_PER_TRIANGLE;
		short[] indices = new short[MAX_INDICES];

		fullScrenMesh = new Mesh(Mesh.VertexDataType.VertexArray, true, MAX_VERTICES, MAX_INDICES,
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
		fullScrenMesh.setVertices(vertices);

		indices[0] = 0; // indices for triangle with vertices [0, 1, 2] [2, 3, 0]
		indices[1] = 1;
		indices[2] = 2;
		indices[3] = 2;
		indices[4] = 3;
		indices[5] = 0;
		fullScrenMesh.setIndices(indices);

		FileHandle vertexShaderFile = Gdx.files.classpath("shaders/combined_lighting_vertex_shader.glsl");
		FileHandle fragmentShaderFile = Gdx.files.classpath("shaders/combined_lighting_fragment_shader.glsl");
		combinedShader = ShaderLoader.createShader(vertexShaderFile, fragmentShaderFile);

		FileHandle edgeDetectingVertexShader = Gdx.files.classpath("shaders/combined_lighting_vertex_shader.glsl");
		FileHandle edgeDetectingFragmentShader = Gdx.files.classpath("shaders/outline_fragment_shader.glsl");
		entityOutlineShader = ShaderLoader.createShader(edgeDetectingVertexShader, edgeDetectingFragmentShader);

		FileHandle dilateImageFragmentShader = Gdx.files.classpath("shaders/dilate_fragment_shader.glsl");
		dilateImageShader = ShaderLoader.createShader(vertexShaderFile, dilateImageFragmentShader);
		new GLProfiler(Gdx.graphics).enable();
	}

	public void renderFinal(TextureRegion diffuseTextureRegion, TextureRegion lightingTextureRegion, float fadeAmount) {
		Gdx.gl.glDisable(GL20.GL_BLEND);

		combinedShader.begin();
		combinedShader.setUniformi("u_textureLighting", 1);
		lightingTextureRegion.getTexture().bind(1);
		combinedShader.setUniformi("u_textureDiffuse", 0);
		diffuseTextureRegion.getTexture().bind(0);
		combinedShader.setUniformf("u_alpha", 1 - fadeAmount);


		fullScrenMesh.render(combinedShader, GL20.GL_TRIANGLES);
		combinedShader.end();
	}

	public void renderstuff(TextureRegion textureRegion) {
		Gdx.gl.glDisable(GL20.GL_BLEND);
		SpriteBatch spriteBatch = new SpriteBatch();
		spriteBatch.begin();
		spriteBatch.draw(textureRegion, 0, 0);
		spriteBatch.end();
	}


	public void renderEntityOutline(TextureRegion textureRegion) {
		Gdx.gl.glDisable(GL20.GL_BLEND);
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		entityOutlineShader.begin();
		entityOutlineShader.setUniformi("u_texture", 0);
		textureRegion.getTexture().bind(0);
		entityOutlineShader.setUniform2fv("u_viewportResolution", new float[] {Gdx.graphics.getWidth(), Gdx.graphics.getHeight() }, 0, 2);
		fullScrenMesh.render(entityOutlineShader, GL20.GL_TRIANGLES);
		entityOutlineShader.end();
	}

	@Override
	public void dispose() {
		combinedShader.dispose();
	}

	public void dilateImage(TextureRegion textureRegion) {
		Gdx.gl.glDisable(GL20.GL_BLEND);

		dilateImageShader.begin();
		dilateImageShader.setUniformi("u_texture", 0);
		textureRegion.getTexture().bind(0);

		fullScrenMesh.render(dilateImageShader, GL20.GL_TRIANGLES);
		dilateImageShader.end();
	}
}
