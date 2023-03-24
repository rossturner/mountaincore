package technology.rocketjump.mountaincore.mapgen.model.input;

public class MushroomType {

	private final String name;
	private final float weighting;

	public MushroomType(String name, float weighting) {
		this.name = name;
		this.weighting = weighting;
	}

	public String getName() {
		return name;
	}

	public float getWeighting() {
		return weighting;
	}
}
