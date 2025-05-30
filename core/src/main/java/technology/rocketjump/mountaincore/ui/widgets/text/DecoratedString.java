package technology.rocketjump.mountaincore.ui.widgets.text;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DecoratedString {

	private final List<DecoratedStringToken> tokens;

	public static DecoratedString fromString(String value) {
		return new DecoratedString(Lists.newArrayList(new DecoratedStringToken(DecoratedStringToken.TokenType.TEXT, value)));
	}

	public static DecoratedString drawable(String value) {
		return new DecoratedString(Lists.newArrayList(new DecoratedStringToken(DecoratedStringToken.TokenType.DRAWABLE, value)));
	}

	public static DecoratedString linebreak() {
		return new DecoratedString(Lists.newArrayList(new DecoratedStringToken(DecoratedStringToken.TokenType.LINEBREAK, null)));
	}

	public static DecoratedString blank() {
		return new DecoratedString(Lists.newArrayList(new DecoratedStringToken(DecoratedStringToken.TokenType.TEXT, "")));
	}

	public static DecoratedString of(DecoratedString... args) {
		List<DecoratedStringToken> tokens = new ArrayList<>();
		for (DecoratedString arg : args) {
			tokens.addAll(arg.tokens);
		}
		return new DecoratedString(tokens);
	}

	private DecoratedString(List<DecoratedStringToken> tokens) {
		this.tokens = tokens;
	}

	public void replace(String textToReplace, DecoratedString replacement) {
		for (int cursor = 0; cursor < tokens.size(); cursor++) {
			DecoratedStringToken token = tokens.get(cursor);
			if (!token.type.equals(DecoratedStringToken.TokenType.TEXT)) {
				continue;
			}
			String tokenText = token.value;
			if (tokenText.contains(textToReplace)) {
				String prefix = "";
				if (tokenText.indexOf(textToReplace) > 0) {
					prefix = tokenText.substring(0, tokenText.indexOf(textToReplace));
				}
				String suffix = "";
				int postIndex = tokenText.indexOf(textToReplace) + textToReplace.length();
				if (postIndex < tokenText.length()) {
					suffix = tokenText.substring(postIndex);
				}

				tokens.add(cursor, new DecoratedStringToken(DecoratedStringToken.TokenType.TEXT, suffix));
				for (int replacementCursor = replacement.tokens.size() - 1; replacementCursor >= 0; replacementCursor--) {
					tokens.add(cursor, replacement.tokens.get(replacementCursor));
				}
				tokens.add(cursor, new DecoratedStringToken(DecoratedStringToken.TokenType.TEXT, prefix));
				tokens.remove(cursor + 2 + (replacement.tokens.size()));

				break;
			}
		}

		tokens.removeIf(token -> token.value != null && token.value.equals(""));
	}

	public void apply(DecorationFlag decorationFlag) {
		for (DecoratedStringToken token : tokens) {
			if (token.value == null) {
				continue;
			}
			switch (decorationFlag) {
				case LOWERCASE -> token.value = token.value.toLowerCase(Locale.ROOT);
				case UPPERCASE -> token.value = token.value.toUpperCase(Locale.ROOT);
				case WORDCASE -> token.value = WordUtils.capitalize(token.value);
			}
		}
	}

	public List<DecoratedStringToken> getTokens() {
		return tokens;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		tokens.forEach(t -> stringBuilder.append(t.toString()));
		return stringBuilder.toString();
	}
}
