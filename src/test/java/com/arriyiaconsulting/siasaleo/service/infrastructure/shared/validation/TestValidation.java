package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

/**
 * A real Bean Validation validator for unit tests. Payara injects its own at
 * runtime; this one uses the parameter-only interpolator so tests need no
 * Expression Language implementation on the classpath.
 */
public final class TestValidation {

    private static final Validator VALIDATOR = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    private TestValidation() {
    }

    public static Validator validator() {
        return VALIDATOR;
    }
}
