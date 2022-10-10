package technology.rocketjump.saul.entities.factories.names;

import com.badlogic.gdx.math.RandomXS128;
import org.apache.commons.io.IOUtils;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GivenNameList {

	private final List<String> maleGivenNames = new ArrayList<>();
	private final List<String> femaleGivenNames = new ArrayList<>();

	public GivenNameList(File givenNamesFile) throws IOException {
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {
			fileReader = new FileReader(givenNamesFile);
			bufferedReader = new BufferedReader(fileReader);

			String line = bufferedReader.readLine();
			while (line != null) {
				String[] splitLine = line.split(",");
				if (splitLine[1].equals("e")) {
					maleGivenNames.add(splitLine[0].trim());
					femaleGivenNames.add(splitLine[0].trim());
				} else if (splitLine[1].equals("f")) {
					femaleGivenNames.add(splitLine[0].trim());
				} else {
					maleGivenNames.add(splitLine[0].trim());
				}

				line = bufferedReader.readLine();
			}
		} finally {
			IOUtils.closeQuietly(fileReader);
			IOUtils.closeQuietly(bufferedReader);
		}
	}

	public String createGivenName(long seed, Gender gender) {
		Random random = new RandomXS128(seed);
		List<String> givenNameList = maleGivenNames;
		if (gender.equals(Gender.FEMALE)) {
			givenNameList = femaleGivenNames;
		}
		return givenNameList.get(random.nextInt(givenNameList.size()));
	}

}
