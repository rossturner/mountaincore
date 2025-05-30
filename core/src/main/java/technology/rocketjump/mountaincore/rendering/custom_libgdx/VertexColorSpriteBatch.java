package technology.rocketjump.mountaincore.rendering.custom_libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

public class VertexColorSpriteBatch implements Disposable {

	private static final int SPRITE_SIZE = 4 * (2 + 1 + 2);
	/**
	 * @deprecated Do not use, this field is for testing only and is likely to be removed. Sets the {@link Mesh.VertexDataType} to be
	 * used when gles 3 is not available, defaults to {@link Mesh.VertexDataType#VertexArray}.
	 */
	@Deprecated
	public static Mesh.VertexDataType defaultVertexDataType = Mesh.VertexDataType.VertexArray;

	private Mesh mesh;

	final float[] vertices;
	int idx = 0;
	Texture lastTexture = null;
	float invTexWidth = 0, invTexHeight = 0;

	boolean drawing = false;

	private final Matrix4 transformMatrix = new Matrix4();
	private final Matrix4 projectionMatrix = new Matrix4();
	private final Matrix4 combinedMatrix = new Matrix4();

	private boolean blendingDisabled = false;
	private int blendSrcFunc = GL20.GL_SRC_ALPHA;
	private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;

	private final ShaderProgram shader;
	private ShaderProgram customShader = null;
	private boolean ownsShader;

	float color = Color.WHITE.toFloatBits();
	private float[] vertexColors = new float[4];

	private Color tempColor = new Color(1, 1, 1, 1);

	/**
	 * Number of render calls since the last {@link #begin()}.
	 **/
	public int renderCalls = 0;

	/**
	 * Number of rendering calls, ever. Will not be reset unless set manually.
	 **/
	public int totalRenderCalls = 0;

	/**
	 * The maximum number of sprites rendered in one batch so far.
	 **/
	public int maxSpritesInBatch = 0;

	/**
	 * Constructs a new SpriteBatch with a size of 1000, one buffer, and the default shader.
	 *
	 * @see com.badlogic.gdx.graphics.g2d.SpriteBatch#SpriteBatch(int, ShaderProgram)
	 */
	public VertexColorSpriteBatch() {
		this(1000, null);
	}

	/**
	 * Constructs a SpriteBatch with one buffer and the default shader.
	 *
	 * @see com.badlogic.gdx.graphics.g2d.SpriteBatch#SpriteBatch(int, ShaderProgram)
	 */
	public VertexColorSpriteBatch(int size) {
		this(size, null);
	}

	/**
	 * Constructs a new SpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point upwards, x-axis
	 * point to the right and the origin being in the bottom left corner of the screen. The projection will be pixel perfect with
	 * respect to the current screen resolution.
	 * <p>
	 * The defaultShader specifies the shader to use. Note that the names for uniforms for this default shader are different than
	 * the ones expect for shaders set with {@link #setShader(ShaderProgram)}. See {@link #createDefaultShader()}.
	 *
	 * @param size          The max number of sprites in a single batch. Max of 5460.
	 * @param defaultShader The default shader to use. This is not owned by the SpriteBatch and must be disposed separately.
	 */
	public VertexColorSpriteBatch(int size, ShaderProgram defaultShader) {
		// 32767 is max index, so 32767 / 6 - (32767 / 6 % 3) = 5460.
		if (size > 5460) throw new IllegalArgumentException("Can't have more than 5460 sprites per batch: " + size);

		Mesh.VertexDataType vertexDataType = (Gdx.gl30 != null) ? Mesh.VertexDataType.VertexBufferObjectWithVAO : defaultVertexDataType;

		mesh = new Mesh(vertexDataType, false, size * 4, size * 6, new VertexAttribute(VertexAttributes.Usage.Position, 2,
				ShaderProgram.POSITION_ATTRIBUTE), new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
				new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		vertices = new float[size * SPRITE_SIZE];

		int len = size * 6;
		short[] indices = new short[len];
		short j = 0;
		for (int i = 0; i < len; i += 6, j += 4) {
			indices[i] = j;
			indices[i + 1] = (short) (j + 1);
			indices[i + 2] = (short) (j + 2);
			indices[i + 3] = (short) (j + 2);
			indices[i + 4] = (short) (j + 3);
			indices[i + 5] = j;
		}
		mesh.setIndices(indices);

		if (defaultShader == null) {
			shader = createDefaultShader();
			ownsShader = true;
		} else
			shader = defaultShader;
	}

	/**
	 * Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified.
	 */
	static public ShaderProgram createDefaultShader() {
		FileHandle vertexShaderFile = ShaderLoader.DEFAULT_VERTEX_SHADER;
		FileHandle fragmentShaderFile = Gdx.files.classpath("shaders/default_fragment_shader.glsl");
		return ShaderLoader.createShader(vertexShaderFile, fragmentShaderFile);
	}

	public void begin() {
		if (drawing) throw new IllegalStateException("SpriteBatch.end must be called before begin.");
		renderCalls = 0;

		Gdx.gl.glDepthMask(false);
		if (customShader != null)
			customShader.bind();
		else
			shader.bind();
		setupMatrices();

		drawing = true;
	}

	public void end() {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before end.");
		if (idx > 0) flush();
		lastTexture = null;
		drawing = false;

		GL20 gl = Gdx.gl;
		gl.glDepthMask(true);
		if (isBlendingEnabled()) gl.glDisable(GL20.GL_BLEND);
	}

	public void setColor(Color color) {
		this.color = color.toFloatBits();
	}

	public void setColors(float[] colors) {
		this.vertexColors = colors;
	}

	public void draw(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX,
					 float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		if (texture != lastTexture)
			switchTexture(texture);
		else if (idx == vertices.length) //
			flush();

		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;

		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}

		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;

		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;

		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);

			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;

			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;

			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;

			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;

			x2 = p2x;
			y2 = p2y;

			x3 = p3x;
			y3 = p3y;

			x4 = p4x;
			y4 = p4y;
		}

		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;

		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;

		if (flipX) {
			float tmp = u;
			u = u2;
			u2 = tmp;
		}

		if (flipY) {
			float tmp = v;
			v = v2;
			v2 = tmp;
		}

		float color = this.color;
		int idx = this.idx;
		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.idx = idx;
	}

	public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth,
					 int srcHeight, boolean flipX, boolean flipY) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		if (texture != lastTexture)
			switchTexture(texture);
		else if (idx == vertices.length) //
			flush();

		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;
		final float fx2 = x + width;
		final float fy2 = y + height;

		if (flipX) {
			float tmp = u;
			u = u2;
			u2 = tmp;
		}

		if (flipY) {
			float tmp = v;
			v = v2;
			v2 = tmp;
		}

		float color = this.color;
		int idx = this.idx;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.idx = idx;
	}

	public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		if (texture != lastTexture)
			switchTexture(texture);
		else if (idx == vertices.length) //
			flush();

		final float u = srcX * invTexWidth;
		final float v = (srcY + srcHeight) * invTexHeight;
		final float u2 = (srcX + srcWidth) * invTexWidth;
		final float v2 = srcY * invTexHeight;
		final float fx2 = x + srcWidth;
		final float fy2 = y + srcHeight;

		float color = this.color;
		int idx = this.idx;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.idx = idx;
	}

	public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		if (texture != lastTexture)
			switchTexture(texture);
		else if (idx == vertices.length) //
			flush();

		final float fx2 = x + width;
		final float fy2 = y + height;

		float color = this.color;
		int idx = this.idx;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.idx = idx;
	}

	public void draw(Texture texture, float x, float y) {
		draw(texture, x, y, texture.getWidth(), texture.getHeight());
	}

	public void draw(Texture texture, float x, float y, float width, float height) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		if (texture != lastTexture)
			switchTexture(texture);
		else if (idx == vertices.length) //
			flush();

		final float fx2 = x + width;
		final float fy2 = y + height;
		final float u = 0;
		final float v = 1;
		final float u2 = 1;
		final float v2 = 0;

		float color = this.color;
		int idx = this.idx;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.idx = idx;
	}

	public void draw(Texture texture, float[] spriteVertices, int offset, int count) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		int verticesLength = vertices.length;
		int remainingVertices = verticesLength;
		if (texture != lastTexture)
			switchTexture(texture);
		else {
			remainingVertices -= idx;
			if (remainingVertices == 0) {
				flush();
				remainingVertices = verticesLength;
			}
		}
		int copyCount = Math.min(remainingVertices, count);

		System.arraycopy(spriteVertices, offset, vertices, idx, copyCount);
		idx += copyCount;
		count -= copyCount;
		while (count > 0) {
			offset += copyCount;
			flush();
			copyCount = Math.min(verticesLength, count);
			System.arraycopy(spriteVertices, offset, vertices, 0, copyCount);
			idx += copyCount;
			count -= copyCount;
		}
	}

	public void draw(TextureRegion region, float x, float y) {
		draw(region, x, y, region.getRegionWidth(), region.getRegionHeight());
	}


	public void draw(TextureRegion region, float x, float y, float width, float height) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		Texture texture = region.getTexture();
		if (texture != lastTexture) {
			switchTexture(texture);
		} else if (idx == vertices.length) //
			flush();

		final float fx2 = x + width;
		final float fy2 = y + height;
		final float u = region.getU();
		final float v = region.getV2();
		final float u2 = region.getU2();
		final float v2 = region.getV();

		float color = this.color;
		int idx = this.idx;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.idx = idx;
	}

	public void draw(TextureRegion region, float x, float y, float width, float height, Color[] vertexColors) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		Texture texture = region.getTexture();
		if (texture != lastTexture) {
			switchTexture(texture);
		} else if (idx == vertices.length) //
			flush();

		final float fx2 = x + width;
		final float fy2 = y + height;
		final float u = region.getU();
		final float v = region.getV2();
		final float u2 = region.getU2();
		final float v2 = region.getV();

		float color = this.color;
		int idx = this.idx;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = vertexColors[0].toFloatBits();
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = vertexColors[1].toFloatBits();
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = vertexColors[2].toFloatBits();
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = vertexColors[3].toFloatBits();
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.idx = idx;
	}

//	public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height,
//					 float scaleX, float scaleY, float rotation) {
//		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
//
//		float[] vertices = this.vertices;
//
//		Texture texture = region.getTexture();
//		if (texture != lastTexture) {
//			switchTexture(texture);
//		} else if (idx == vertices.length) //
//			flush();
//
//		// bottom left and top right corner points relative to origin
//		final float worldOriginX = x + originX;
//		final float worldOriginY = y + originY;
//		float fx = -originX;
//		float fy = -originY;
//		float fx2 = width - originX;
//		float fy2 = height - originY;
//
//		// scale
//		if (scaleX != 1 || scaleY != 1) {
//			fx *= scaleX;
//			fy *= scaleY;
//			fx2 *= scaleX;
//			fy2 *= scaleY;
//		}
//
//		// construct corner points, start from top left and go counter clockwise
//		final float p1x = fx;
//		final float p1y = fy;
//		final float p2x = fx;
//		final float p2y = fy2;
//		final float p3x = fx2;
//		final float p3y = fy2;
//		final float p4x = fx2;
//		final float p4y = fy;
//
//		float x1;
//		float y1;
//		float x2;
//		float y2;
//		float x3;
//		float y3;
//		float x4;
//		float y4;
//
//		// rotate
//		if (rotation != 0) {
//			final float cos = MathUtils.cosDeg(rotation);
//			final float sin = MathUtils.sinDeg(rotation);
//
//			x1 = cos * p1x - sin * p1y;
//			y1 = sin * p1x + cos * p1y;
//
//			x2 = cos * p2x - sin * p2y;
//			y2 = sin * p2x + cos * p2y;
//
//			x3 = cos * p3x - sin * p3y;
//			y3 = sin * p3x + cos * p3y;
//
//			x4 = x1 + (x3 - x2);
//			y4 = y3 - (y2 - y1);
//		} else {
//			x1 = p1x;
//			y1 = p1y;
//
//			x2 = p2x;
//			y2 = p2y;
//
//			x3 = p3x;
//			y3 = p3y;
//
//			x4 = p4x;
//			y4 = p4y;
//		}
//
//		x1 += worldOriginX;
//		y1 += worldOriginY;
//		x2 += worldOriginX;
//		y2 += worldOriginY;
//		x3 += worldOriginX;
//		y3 += worldOriginY;
//		x4 += worldOriginX;
//		y4 += worldOriginY;
//
//		final float u = region.u;
//		final float v = region.v2;
//		final float u2 = region.u2;
//		final float v2 = region.v;
//
//		float color = this.color;
//		int idx = this.idx;
//		vertices[idx++] = x1;
//		vertices[idx++] = y1;
//		vertices[idx++] = color;
//		vertices[idx++] = u;
//		vertices[idx++] = v;
//
//		vertices[idx++] = x2;
//		vertices[idx++] = y2;
//		vertices[idx++] = color;
//		vertices[idx++] = u;
//		vertices[idx++] = v2;
//
//		vertices[idx++] = x3;
//		vertices[idx++] = y3;
//		vertices[idx++] = color;
//		vertices[idx++] = u2;
//		vertices[idx++] = v2;
//
//		vertices[idx++] = x4;
//		vertices[idx++] = y4;
//		vertices[idx++] = color;
//		vertices[idx++] = u2;
//		vertices[idx++] = v;
//		this.idx = idx;
//	}

//	@Override
//	public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height,
//					 float scaleX, float scaleY, float rotation, boolean clockwise) {
//		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
//
//		float[] vertices = this.vertices;
//
//		Texture texture = region.texture;
//		if (texture != lastTexture) {
//			switchTexture(texture);
//		} else if (idx == vertices.length) //
//			flush();
//
//		// bottom left and top right corner points relative to origin
//		final float worldOriginX = x + originX;
//		final float worldOriginY = y + originY;
//		float fx = -originX;
//		float fy = -originY;
//		float fx2 = width - originX;
//		float fy2 = height - originY;
//
//		// scale
//		if (scaleX != 1 || scaleY != 1) {
//			fx *= scaleX;
//			fy *= scaleY;
//			fx2 *= scaleX;
//			fy2 *= scaleY;
//		}
//
//		// construct corner points, start from top left and go counter clockwise
//		final float p1x = fx;
//		final float p1y = fy;
//		final float p2x = fx;
//		final float p2y = fy2;
//		final float p3x = fx2;
//		final float p3y = fy2;
//		final float p4x = fx2;
//		final float p4y = fy;
//
//		float x1;
//		float y1;
//		float x2;
//		float y2;
//		float x3;
//		float y3;
//		float x4;
//		float y4;
//
//		// rotate
//		if (rotation != 0) {
//			final float cos = MathUtils.cosDeg(rotation);
//			final float sin = MathUtils.sinDeg(rotation);
//
//			x1 = cos * p1x - sin * p1y;
//			y1 = sin * p1x + cos * p1y;
//
//			x2 = cos * p2x - sin * p2y;
//			y2 = sin * p2x + cos * p2y;
//
//			x3 = cos * p3x - sin * p3y;
//			y3 = sin * p3x + cos * p3y;
//
//			x4 = x1 + (x3 - x2);
//			y4 = y3 - (y2 - y1);
//		} else {
//			x1 = p1x;
//			y1 = p1y;
//
//			x2 = p2x;
//			y2 = p2y;
//
//			x3 = p3x;
//			y3 = p3y;
//
//			x4 = p4x;
//			y4 = p4y;
//		}
//
//		x1 += worldOriginX;
//		y1 += worldOriginY;
//		x2 += worldOriginX;
//		y2 += worldOriginY;
//		x3 += worldOriginX;
//		y3 += worldOriginY;
//		x4 += worldOriginX;
//		y4 += worldOriginY;
//
//		float u1, v1, u2, v2, u3, v3, u4, v4;
//		if (clockwise) {
//			u1 = region.u2;
//			v1 = region.v2;
//			u2 = region.u;
//			v2 = region.v2;
//			u3 = region.u;
//			v3 = region.v;
//			u4 = region.u2;
//			v4 = region.v;
//		} else {
//			u1 = region.u;
//			v1 = region.v;
//			u2 = region.u2;
//			v2 = region.v;
//			u3 = region.u2;
//			v3 = region.v2;
//			u4 = region.u;
//			v4 = region.v2;
//		}
//
//		float color = this.color;
//		int idx = this.idx;
//		vertices[idx++] = x1;
//		vertices[idx++] = y1;
//		vertices[idx++] = color;
//		vertices[idx++] = u1;
//		vertices[idx++] = v1;
//
//		vertices[idx++] = x2;
//		vertices[idx++] = y2;
//		vertices[idx++] = color;
//		vertices[idx++] = u2;
//		vertices[idx++] = v2;
//
//		vertices[idx++] = x3;
//		vertices[idx++] = y3;
//		vertices[idx++] = color;
//		vertices[idx++] = u3;
//		vertices[idx++] = v3;
//
//		vertices[idx++] = x4;
//		vertices[idx++] = y4;
//		vertices[idx++] = color;
//		vertices[idx++] = u4;
//		vertices[idx++] = v4;
//		this.idx = idx;
//	}

//	@Override
//	public void draw(TextureRegion region, float width, float height, Affine2 transform) {
//		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
//
//		float[] vertices = this.vertices;
//
//		Texture texture = region.texture;
//		if (texture != lastTexture) {
//			switchTexture(texture);
//		} else if (idx == vertices.length) {
//			flush();
//		}
//
//		// construct corner points
//		float x1 = transform.m02;
//		float y1 = transform.m12;
//		float x2 = transform.m01 * height + transform.m02;
//		float y2 = transform.m11 * height + transform.m12;
//		float x3 = transform.m00 * width + transform.m01 * height + transform.m02;
//		float y3 = transform.m10 * width + transform.m11 * height + transform.m12;
//		float x4 = transform.m00 * width + transform.m02;
//		float y4 = transform.m10 * width + transform.m12;
//
//		float u = region.u;
//		float v = region.v2;
//		float u2 = region.u2;
//		float v2 = region.v;
//
//		float color = this.color;
//		int idx = this.idx;
//		vertices[idx++] = x1;
//		vertices[idx++] = y1;
//		vertices[idx++] = color;
//		vertices[idx++] = u;
//		vertices[idx++] = v;
//
//		vertices[idx++] = x2;
//		vertices[idx++] = y2;
//		vertices[idx++] = color;
//		vertices[idx++] = u;
//		vertices[idx++] = v2;
//
//		vertices[idx++] = x3;
//		vertices[idx++] = y3;
//		vertices[idx++] = color;
//		vertices[idx++] = u2;
//		vertices[idx++] = v2;
//
//		vertices[idx++] = x4;
//		vertices[idx++] = y4;
//		vertices[idx++] = color;
//		vertices[idx++] = u2;
//		vertices[idx++] = v;
//		this.idx = idx;
//	}

	public void flush() {
		if (idx == 0) return;

		renderCalls++;
		totalRenderCalls++;
		int spritesInBatch = idx / SPRITE_SIZE;
		if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
		int count = spritesInBatch * 6;

		lastTexture.bind();
		Mesh mesh = this.mesh;
		mesh.setVertices(vertices, 0, idx);
		mesh.getIndicesBuffer().position(0);
		mesh.getIndicesBuffer().limit(count);

		if (blendingDisabled) {
			Gdx.gl.glDisable(GL20.GL_BLEND);
		} else {
			Gdx.gl.glEnable(GL20.GL_BLEND);
			if (blendSrcFunc != -1) Gdx.gl.glBlendFunc(blendSrcFunc, blendDstFunc);
		}

		mesh.render(customShader != null ? customShader : shader, GL20.GL_TRIANGLES, 0, count);

		idx = 0;
	}

	public void disableBlending() {
		if (blendingDisabled) return;
		flush();
		blendingDisabled = true;
	}

	public void enableBlending() {
		if (!blendingDisabled) return;
		flush();
		blendingDisabled = false;
	}

	public void setBlendFunction(int srcFunc, int dstFunc) {
		if (blendSrcFunc == srcFunc && blendDstFunc == dstFunc) return;
		flush();
		blendSrcFunc = srcFunc;
		blendDstFunc = dstFunc;
	}

	public int getBlendSrcFunc() {
		return blendSrcFunc;
	}

	public int getBlendDstFunc() {
		return blendDstFunc;
	}

	public void dispose() {
		mesh.dispose();
		if (ownsShader && shader != null) shader.dispose();
		if (customShader != null) {
			customShader.dispose();
		}
		if (lastTexture != null) {
			lastTexture.dispose();
		}
	}

	public Matrix4 getProjectionMatrix() {
		return projectionMatrix;
	}

	public Matrix4 getTransformMatrix() {
		return transformMatrix;
	}

	public void setProjectionMatrix(Matrix4 projection) {
		if (drawing) flush();
		projectionMatrix.set(projection);
		if (drawing) setupMatrices();
	}

	public void setTransformMatrix(Matrix4 transform) {
		if (drawing) flush();
		transformMatrix.set(transform);
		if (drawing) setupMatrices();
	}

	private void setupMatrices() {
		combinedMatrix.set(projectionMatrix).mul(transformMatrix);
		if (customShader != null) {
			customShader.setUniformMatrix("u_projTrans", combinedMatrix);
			customShader.setUniformi("u_texture", 0);
		} else {
			shader.setUniformMatrix("u_projTrans", combinedMatrix);
			shader.setUniformi("u_texture", 0);
		}
	}

	protected void switchTexture(Texture texture) {
		flush();
		lastTexture = texture;
		invTexWidth = 1.0f / texture.getWidth();
		invTexHeight = 1.0f / texture.getHeight();
	}

	public void setShader(ShaderProgram shader) {
		if (drawing) {
			flush();
		}
		customShader = shader;
		if (drawing) {
			if (customShader != null)
				customShader.bind();
			else
				this.shader.bind();
			setupMatrices();
		}
	}

	public ShaderProgram getShader() {
		if (customShader == null) {
			return shader;
		}
		return customShader;
	}

	public boolean isBlendingEnabled() {
		return !blendingDisabled;
	}

	public boolean isDrawing() {
		return drawing;
	}
}
