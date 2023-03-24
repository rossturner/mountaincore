package technology.rocketjump.mountaincore.launcher.spritecropper;

import technology.rocketjump.mountaincore.assets.editor.SpriteCropper;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SpriteCropperLauncher {

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new IllegalArgumentException("This class must be passed a single argument of a directory to process");
		}
		Path filePath = Paths.get(new URL("file:///"+args[0]).toURI());
		new SpriteCropper().processDirectory(filePath);
	}
}
