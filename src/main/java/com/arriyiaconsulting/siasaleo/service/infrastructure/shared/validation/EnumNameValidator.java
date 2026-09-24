package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class EnumNameValidator implements ConstraintValidator<EnumName, String> {

    private List<String> names;

    @Override
    public void initialize(EnumName annotation) {
        names = Arrays.stream(annotation.value().getEnumConstants()).map(Enum::name).toList();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || names.contains(value)) {
            return true;
        }
        // Name the allowed values, which the static message cannot. They are
        // enum constant names, so safe to use as a message template.
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("must be one of " + String.join(", ", names))
                .addConstraintViolation();
        return false;
    }
}
