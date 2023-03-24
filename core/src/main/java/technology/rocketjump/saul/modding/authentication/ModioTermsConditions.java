package technology.rocketjump.saul.modding.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.ui.i18n.I18nRepo;

import java.io.IOException;
import java.util.function.Consumer;

@Singleton
public class ModioTermsConditions {

	private final I18nRepo i18nRepo;
	private final ModioRequestAdapter modioRequestAdapter;

	private boolean termsAccepted;

	@Inject
	public ModioTermsConditions(I18nRepo i18nRepo, ModioRequestAdapter modioRequestAdapter) {
		this.i18nRepo = i18nRepo;
		this.modioRequestAdapter = modioRequestAdapter;
	}

	public void getTermsConditionsContent(Consumer<TermsConditionsContent> onSuccess, Runnable onFailure) {
		modioRequestAdapter.termsConditions(i18nRepo.getCurrentLanguageType().getCode(), new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				Logger.error(e, "Failed to get terms and conditions");
				onFailure.run();
			}

			@Override
			public void onResponse(Call call, Response response) throws java.io.IOException {
				if (response.isSuccessful()) {
					String responseString = response.body().string();
					TermsConditionsContent content = new ObjectMapper().readValue(responseString, TermsConditionsContent.class);
					onSuccess.accept(content);
				} else {
					Logger.error("Failed to get terms and conditions: " + response.code() + " " + response.message());
					onFailure.run();
				}
			}
		});
	}

	public boolean isTermsAccepted() {
		return termsAccepted;
	}

	public void setTermsAccepted(boolean termsAccepted) {
		this.termsAccepted = termsAccepted;
	}
}
