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
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import technology.rocketjump.saul.rendering.custom_libgdx.ShaderLoader;

public class CombinedLightingResultRenderer implements Disposable {

	private static final int NUM_TRIANGLES = 2;
	private static final int VERTEX_SIZE = 2;
	private static final int NUM_INDEX_PER_TRIANGLE = 3;
	private final ShaderProgram combinedShader;
//	private final ShaderProgram overlayShader;
	private Mesh fullScreenMesh;

	@Inject
	public CombinedLightingResultRenderer() {
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

		FileHandle vertexShaderFile = Gdx.files.classpath("shaders/combined_lighting_vertex_shader.glsl");
		FileHandle fragmentShaderFile = Gdx.files.classpath("shaders/combined_lighting_fragment_shader.glsl");
		combinedShader = ShaderLoader.createShader(vertexShaderFile, fragmentShaderFile);


//		FileHandle fragmentShaderFile = Gdx.files.classpath("shaders/combined_lighting_fragment_shader.glsl");
//		overlayShader = ShaderLoader.createShader(vertexShaderFile, fragmentShaderFile);
	}

	public void renderFinal(TextureRegion diffuseTextureRegion, TextureRegion lightingTextureRegion, float fadeAmount) {
		Gdx.gl.glDisable(GL20.GL_BLEND);

		combinedShader.begin();
		combinedShader.setUniformi("u_textureLighting", 1);
		lightingTextureRegion.getTexture().bind(1);
		combinedShader.setUniformi("u_textureDiffuse", 0);
		diffuseTextureRegion.getTexture().bind(0);
		combinedShader.setUniformf("u_alpha", 1 - fadeAmount);

		fullScreenMesh.render(combinedShader, GL20.GL_TRIANGLES);
		combinedShader.end();
	}

	//TODO: this is dirty
	public void renderstuff(TextureRegion overlay, TextureRegion toSubtract) {

//		overlayShader.begin();
//		overlayShader.setUniformi("u_overlay", 1);
//		overlay.getTexture().bind(1);
//		overlayShader.setUniformi("u_toSubtract", 0);
//		toSubtract.getTexture().bind(0);
//
//		fullScreenMesh.render(overlayShader, GL20.GL_TRIANGLES);
//		overlayShader.end();

		SpriteBatch spriteBatch = new SpriteBatch();
		spriteBatch.enableBlending();
		spriteBatch.begin();
		spriteBatch.draw(overlay, 0, 0);
		spriteBatch.end();
	}

	@Override
	public void dispose() {
		combinedShader.dispose();
	}
}
