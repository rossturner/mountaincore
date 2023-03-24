package technology.rocketjump.mountaincore.entities.factories.names;

public interface LoadedFromCsv {

	void readFromLine(String line);

	String writeToLine();

	String getUniqueName();

}
