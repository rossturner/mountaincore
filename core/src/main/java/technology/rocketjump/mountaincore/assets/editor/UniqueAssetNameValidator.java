package technology.rocketjump.mountaincore.assets.editor;


import com.kotcrab.vis.ui.util.InputValidator;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.mountaincore.assets.entities.CompleteAssetDictionary;

public class UniqueAssetNameValidator implements InputValidator {
    private final CompleteAssetDictionary completeAssetDictionary;

    public UniqueAssetNameValidator(CompleteAssetDictionary completeAssetDictionary) {
        this.completeAssetDictionary = completeAssetDictionary;
    }

    @Override
    public boolean validateInput(String input) {
        if (StringUtils.isBlank(input)) {
            return false;
        }
        return completeAssetDictionary.getByUniqueName(input) == null;
    }
}
