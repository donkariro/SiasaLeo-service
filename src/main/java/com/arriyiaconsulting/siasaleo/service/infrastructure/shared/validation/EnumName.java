package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The text must be the name of one of the enum's constants. For request
 * fields that stay text on the wire (so import fingerprints stay stable) but
 * whose allowed values an enum already defines. Null is valid; pair with
 * NotNull when the field is required.
 */
@Documented
@Constraint(validatedBy = EnumNameValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumName {

    Class<? extends Enum<?>> value();

    String message() default "must be one of the allowed values";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
