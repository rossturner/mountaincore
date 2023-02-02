package technology.rocketjump.saul.assets.entities.model;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;
import technology.rocketjump.saul.entities.model.EntityType;

import static org.fest.assertions.Assertions.assertThat;
import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.*;

public class EntityAssetOrientationTest {

	@Test
	public void fromFacing_returnsCorrectOrientation() {
		Vector2 NORTH = new Vector2(0f, 1f).nor();
		assertThat(fromFacing(NORTH, EntityType.CREATURE)).isEqualTo(UP);

		Vector2 NORTH_WEST = new Vector2(-1f, 1f).nor();
		assertThat(fromFacing(NORTH_WEST, EntityType.CREATURE)).isEqualTo(UP_LEFT);

		Vector2 WEST = new Vector2(-1f, 0f).nor();
		assertThat(fromFacing(WEST, EntityType.CREATURE)).isEqualTo(DOWN_LEFT);

		Vector2 SOUTH_WEST = new Vector2(-1f, -1f).nor();
		assertThat(fromFacing(SOUTH_WEST, EntityType.CREATURE)).isEqualTo(DOWN_LEFT);

		Vector2 SOUTH = new Vector2(0f, -1f).nor();
		assertThat(fromFacing(SOUTH, EntityType.CREATURE)).isEqualTo(DOWN);

		Vector2 SOUTH_EAST = new Vector2(1f, -1f).nor();
		assertThat(fromFacing(SOUTH_EAST, EntityType.CREATURE)).isEqualTo(DOWN_RIGHT);

		Vector2 EAST = new Vector2(1f, 0f).nor();
		assertThat(fromFacing(EAST, EntityType.CREATURE)).isEqualTo(DOWN_RIGHT);

		Vector2 NORTH_EAST = new Vector2(1f, 1f).nor();
		assertThat(fromFacing(NORTH_EAST, EntityType.CREATURE)).isEqualTo(UP_RIGHT);
	}

	@Test
	public void fromFacingTo8Directions_returnsCorrectOrientation() {
		Vector2 NORTH = new Vector2(0f, 1f).nor();
		assertThat(fromFacingTo8Directions(NORTH)).isEqualTo(UP);

		Vector2 NORTH_WEST = new Vector2(-1f, 1f).nor();
		assertThat(fromFacingTo8Directions(NORTH_WEST)).isEqualTo(UP_LEFT);

		Vector2 WEST = new Vector2(-1f, 0f).nor();
		assertThat(fromFacingTo8Directions(WEST)).isEqualTo(LEFT);

		Vector2 SOUTH_WEST = new Vector2(-1f, -1f).nor();
		assertThat(fromFacingTo8Directions(SOUTH_WEST)).isEqualTo(DOWN_LEFT);

		Vector2 SOUTH = new Vector2(0f, -1f).nor();
		assertThat(fromFacingTo8Directions(SOUTH)).isEqualTo(DOWN);

		Vector2 SOUTH_EAST = new Vector2(1f, -1f).nor();
		assertThat(fromFacingTo8Directions(SOUTH_EAST)).isEqualTo(DOWN_RIGHT);

		Vector2 EAST = new Vector2(1f, 0f).nor();
		assertThat(fromFacingTo8Directions(EAST)).isEqualTo(RIGHT);

		Vector2 NORTH_EAST = new Vector2(1f, 1f).nor();
		assertThat(fromFacingTo8Directions(NORTH_EAST)).isEqualTo(UP_RIGHT);
	}

}