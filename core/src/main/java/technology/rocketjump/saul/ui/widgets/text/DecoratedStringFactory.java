package technology.rocketjump.saul.ui.widgets.text;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.ui.i18n.I18nLanguageDictionary;
import technology.rocketjump.saul.ui.i18n.I18nRepo;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.i18n.I18nWordClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static technology.rocketjump.saul.ui.widgets.text.DecoratedStringToken.TokenType.*;

@Singleton
public class DecoratedStringFactory {

	private final I18nRepo i18nRepo;
	private I18nLanguageDictionary dictionary;

	private static final String REGEX_START = Pattern.quote("{{");
	private static final String REGEX_END = Pattern.quote("}}");
	private static final Pattern pattern = Pattern.compile(REGEX_START + "([|\\w\\\\.\\n]+)" + REGEX_END);

	@Inject
	public DecoratedStringFactory(I18nRepo i18nRepo) {
		this.i18nRepo = i18nRepo;
		this.dictionary = i18nRepo.getCurrentLanguage();
	}

	public DecoratedString translate(String i18nKey) {
		return translate(i18nKey, Map.of());
	}

	public DecoratedString translate(String i18nKey, Map<String, DecoratedString> staticReplacements) {
		return translate(i18nKey, staticReplacements, I18nWordClass.UNSPECIFIED);
	}

	public DecoratedString translate(String i18nKey, Map<String, DecoratedString> staticReplacements, I18nWordClass wordClass) {
		I18nWord word = dictionary.getWord(i18nKey);
		DecoratedString result = DecoratedString.fromString(word.get(wordClass));

		Matcher matcher = pattern.matcher(result.toString());
		while (matcher.find()) {
			String matchedGroup = matcher.group(0);
			matchedGroup = matchedGroup.substring("{{".length(), matchedGroup.length() - "}}".length());

			DecoratedStringToken.TokenType tokenType = TEXT;
			List<DecorationFlag> flags = new ArrayList<>();
			while (matchedGroup.contains("|")) {
				String tokenPrefix = matchedGroup.substring(0, matchedGroup.indexOf("|")).toLowerCase();

				switch (tokenPrefix) {
					case "drawable" -> tokenType = DRAWABLE;
					case "uppercase" -> flags.add(DecorationFlag.UPPERCASE);
					case "lowercase" -> flags.add(DecorationFlag.LOWERCASE);
					case "wordcase" -> flags.add(DecorationFlag.WORDCASE);
					default -> Logger.error(String.format("Unknown prefix %s in text %s from key %s", tokenPrefix, result, i18nKey));
				}

				matchedGroup = matchedGroup.substring(matchedGroup.indexOf("|") + 1);
			}

			if (matchedGroup.equals("\\n") || matchedGroup.equals("\n") || matchedGroup.equals("newline")) {
				tokenType = DecoratedStringToken.TokenType.LINEBREAK;
			}

			DecoratedString replacement;
			if (tokenType.equals(DRAWABLE)) {
				replacement = DecoratedString.drawable(matchedGroup);
			} else if (tokenType.equals(LINEBREAK)) {
				replacement = DecoratedString.linebreak();
			} else {
				// figure out replacement text
				if (staticReplacements.containsKey(matchedGroup)) {
					replacement = staticReplacements.get(matchedGroup);
				} else {
					I18nWordClass replacementWordClass = I18nWordClass.UNSPECIFIED;
					String[] parts = matchedGroup.split("\\.");
					String lastPart = parts[parts.length - 1].toUpperCase();
					I18nWordClass parsedWordClass = EnumUtils.getEnum(I18nWordClass.class, lastPart);
					String[] partsWithoutWordClass = parts;
					if (parsedWordClass != null) {
						partsWithoutWordClass = Arrays.copyOfRange(parts, 0, parts.length - 1);
						replacementWordClass = parsedWordClass;
					}

					String rejoinedParts = StringUtils.join(partsWithoutWordClass, ".").toUpperCase();
					replacement = translate(rejoinedParts, staticReplacements, replacementWordClass);
				}
			}

			flags.forEach(replacement::apply);

			result.replace(matcher.group(0), replacement);
		}

		return result;
	}

	public void preLanguageUpdated() {
		dictionary = i18nRepo.getCurrentLanguage();
	}
}
