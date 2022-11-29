package technology.rocketjump.saul.ui.widgets.text;

public class DecoratedStringToken {

	public final TokenType type;
	public String value;

	public DecoratedStringToken(TokenType type, String value) {
		this.type = type;
		this.value = value;
	}

	public enum TokenType {

		TEXT,
		LINEBREAK,
		DRAWABLE

	}

	@Override
	public String toString() {
		return switch (type) {
			case TEXT -> value;
			case DRAWABLE -> "{drawable|" + value + "}";
			case LINEBREAK -> "\n";
		};
	}

}
