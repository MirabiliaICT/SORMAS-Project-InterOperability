package de.symeda.sormas.app.validation;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;

import de.symeda.sormas.api.symptoms.SymptomState;
import de.symeda.sormas.api.utils.ValidationException;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.databinding.FragmentSymptomsEditLayoutBinding;
import de.symeda.sormas.app.util.Callback;

public final class SymptomsValidator {

    public static void initializeSymptomsValidation(final FragmentSymptomsEditLayoutBinding contentBinding) {
        Callback temperatureCallback = new Callback() {
            public void call() {
                if (contentBinding.symptomsFever.getVisibility() == View.VISIBLE) {
                    if (contentBinding.symptomsTemperature.getValue() != null
                            && (Float) contentBinding.symptomsTemperature.getValue() >= 38.0f) {
                        if (contentBinding.symptomsFever.getValue() != SymptomState.YES) {
                            contentBinding.symptomsFever.enableErrorState(null,
                                    R.string.validation_symptoms_fever);
                            return;
                        }
                    }
                }

                contentBinding.symptomsFever.disableErrorState();
            }
        };

        contentBinding.symptomsTemperature.setValidationCallback(temperatureCallback);
        contentBinding.symptomsFever.setValidationCallback(temperatureCallback);
    }

}
