/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package technology.rocketjump.mountaincore.mapgen.noise;

import java.util.Random;

/** Adapted from <a href="http://devmag.org.za/2009/04/25/perlin-noise/">http://devmag.org.za/2009/04/25/perlin-noise/</a>
 * @author badlogic */
public class PerlinNoiseGenerator {

	private Random random;

	public PerlinNoiseGenerator(Random random) {
		this.random = random;
	}

	public float[][] generateWhiteNoise (int width, int height) {
		float[][] noise = new float[width][height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				noise[x][y] = random.nextFloat();
			}
		}
		return noise;
	}

	public float interpolate (float x0, float x1, float alpha) {
		return x0 * (1 - alpha) + alpha * x1;
	}

	public float[][] generateSmoothNoise (float[][] baseNoise, int octave) {
		int width = baseNoise.length;
		int height = baseNoise[0].length;
		float[][] smoothNoise = new float[width][height];

		int samplePeriod = 1 << octave; // calculates 2 ^ k
		float sampleFrequency = 1.0f / samplePeriod;
		for (int i = 0; i < width; i++) {
			int sample_i0 = (i / samplePeriod) * samplePeriod;
			int sample_i1 = (sample_i0 + samplePeriod) % width; // wrap around
			float horizontal_blend = (i - sample_i0) * sampleFrequency;

			for (int j = 0; j < height; j++) {
				int sample_j0 = (j / samplePeriod) * samplePeriod;
				int sample_j1 = (sample_j0 + samplePeriod) % height; // wrap around
				float vertical_blend = (j - sample_j0) * sampleFrequency;
				float top = interpolate(baseNoise[sample_i0][sample_j0], baseNoise[sample_i1][sample_j0], horizontal_blend);
				float bottom = interpolate(baseNoise[sample_i0][sample_j1], baseNoise[sample_i1][sample_j1], horizontal_blend);
				smoothNoise[i][j] = interpolate(top, bottom, vertical_blend);
			}
		}

		return smoothNoise;
	}

	public float[][] generatePerlinNoise (float[][] baseNoise, int octaveCount) {
		int width = baseNoise.length;
		int height = baseNoise[0].length;
		float[][][] smoothNoise = new float[octaveCount][][]; // an array of 2D arrays containing
		float persistance = 0.7f;

		for (int i = 0; i < octaveCount; i++) {
			smoothNoise[i] = generateSmoothNoise(baseNoise, i);
		}

		float[][] perlinNoise = new float[width][height]; // an array of floats initialised to 0

		float amplitude = 1.0f;
		float totalAmplitude = 0.0f;

		for (int octave = octaveCount - 1; octave >= 0; octave--) {
			amplitude *= persistance;
			totalAmplitude += amplitude;

			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					perlinNoise[i][j] += smoothNoise[octave][i][j] * amplitude;
				}
			}
		}

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				perlinNoise[i][j] /= totalAmplitude;
			}
		}

		return perlinNoise;
	}

	public float[][] generatePerlinNoise (int width, int height, int octaveCount) {
		float[][] baseNoise = generateWhiteNoise(width, height);
		return generatePerlinNoise(baseNoise, octaveCount);
	}

	public byte[] generateHeightMap (int width, int height, int min, int max, int octaveCount) {
		float[][] baseNoise = generateWhiteNoise(width, height);
		float[][] noise = generatePerlinNoise(baseNoise, octaveCount);
		byte[] bytes = new byte[baseNoise.length * baseNoise[0].length];
		int idx = 0;
		int range = max - min;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				bytes[idx++] = (byte)(noise[x][y] * range + min);
			}
		}
		return bytes;
	}

}