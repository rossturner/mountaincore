package technology.rocketjump.mountaincore.rendering.custom_libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class ShaderLoader {

	public static final FileHandle DEFAULT_VERTEX_SHADER = Gdx.files.classpath("shaders/default_vertex_shader.glsl");
	public static ShaderProgram defaultShaderInstance;

	static {
		FileHandle fragmentShaderFile = Gdx.files.classpath("shaders/default_fragment_shader.glsl");
		defaultShaderInstance = ShaderLoader.createShader(DEFAULT_VERTEX_SHADER, fragmentShaderFile);
	}

	public static ShaderProgram createShader(FileHandle vertexShaderFile, FileHandle fragmentShaderFile) {
		ShaderProgram shaderProgram = new ShaderProgram(vertexShaderFile, fragmentShaderFile);
		ShaderProgram.pedantic = false;
		if (!shaderProgram.isCompiled()) {
			throw new IllegalArgumentException("couldn't compile shader: "
					+ shaderProgram.getLog());
		}
		return shaderProgram;
	}
}
