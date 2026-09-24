package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error;

import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.validation.TestValidation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstraintViolationExceptionMapperTest {

    record Row(@NotNull Long areaId, @PositiveOrZero Long count) {
    }

    record Batch(@NotNull String source, List<@NotNull @Valid Row> rows) {
    }

    private static List<String> fields(Batch batch) {
        return TestValidation.validator().validate(batch).stream()
                .map(ConstraintViolationExceptionMapper::fieldOf)
                .sorted()
                .toList();
    }

    @Test
    void topLevelFieldIsItsName() {
        assertEquals(List.of("source"), fields(new Batch(null, List.of())));
    }

    @Test
    void rowFieldsKeepTheirListPosition() {
        Batch batch = new Batch("s", List.of(new Row(1L, 0L), new Row(null, -1L)));
        assertEquals(List.of("rows[1].areaId", "rows[1].count"), fields(batch));
    }

    @Test
    void missingRowIsReportedAtItsPosition() {
        assertEquals(List.of("rows[0]"), fields(new Batch("s", Arrays.asList((Row) null))));
    }
}
