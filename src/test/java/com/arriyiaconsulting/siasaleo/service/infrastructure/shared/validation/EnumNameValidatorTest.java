package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.validation;

import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.Coverage;
import jakarta.validation.ConstraintViolation;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnumNameValidatorTest {

    record Sample(@EnumName(Coverage.class) String coverage) {
    }

    private static List<String> messages(String coverage) {
        return TestValidation.validator().validate(new Sample(coverage)).stream()
                .map(ConstraintViolation::getMessage).toList();
    }

    @Test
    void acceptsConstantNamesAndNull() {
        assertTrue(messages("COMPLETE").isEmpty());
        assertTrue(messages("PARTIAL").isEmpty());
        assertTrue(messages(null).isEmpty());
    }

    @Test
    void rejectsOtherTextNamingTheAllowedValues() {
        assertEquals(List.of("must be one of COMPLETE, PARTIAL"), messages("complete"));
        assertEquals(List.of("must be one of COMPLETE, PARTIAL"), messages(""));
    }
}
