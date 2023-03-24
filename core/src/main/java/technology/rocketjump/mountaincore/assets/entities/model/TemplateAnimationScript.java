package technology.rocketjump.mountaincore.assets.entities.model;

import technology.rocketjump.mountaincore.misc.Name;

import java.util.HashMap;
import java.util.Map;

public class TemplateAnimationScript {
	@Name
	private String name;
	private AnimationScript template;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AnimationScript getTemplate() {
		return template;
	}

	public void setTemplate(AnimationScript template) {
		this.template = template;
	}

	public static class Variables {
		private String use;
		private Map<String, String> variables = new HashMap<>();

		public String getUse() {
			return use;
		}

		public void setUse(String use) {
			this.use = use;
		}

		public Map<String, String> getVariables() {
			return variables;
		}

		public void setVariables(Map<String, String> variables) {
			this.variables = variables;
		}
	}
}
