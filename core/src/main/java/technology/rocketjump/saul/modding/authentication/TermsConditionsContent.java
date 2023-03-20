package technology.rocketjump.saul.modding.authentication;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TermsConditionsContent {

	private String plaintext;
	private String html;
	private Map<ModioTCsButton, String> buttons;
	private Map<ModioTCsLinkName, ModioTCsLink> links;

	public String getPlaintext() {
		return plaintext;
	}

	public void setPlaintext(String plaintext) {
		this.plaintext = plaintext;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public Map<ModioTCsButton, String> getButtons() {
		return buttons;
	}

	public void setButtons(Map<ModioTCsButton, String> buttons) {
		this.buttons = buttons;
	}

	public Map<ModioTCsLinkName, ModioTCsLink> getLinks() {
		return links;
	}

	public void setLinks(Map<ModioTCsLinkName, ModioTCsLink> links) {
		this.links = links;
	}

	public enum ModioTCsButton {
		agree, disagree
	}

	public enum ModioTCsLinkName {
		website,
		terms,
		privacy,
		manage
	}

	public static class ModioTCsLink {

		private String text;
		private String url;
		private boolean required;

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public boolean isRequired() {
			return required;
		}

		public void setRequired(boolean required) {
			this.required = required;
		}
	}
}
