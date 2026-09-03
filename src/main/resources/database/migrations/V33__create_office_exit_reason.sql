-- V33: office_exit_reason — lookup table for why an occupancy ended, the
-- counterpart to office_entry_reason (V31). It applies to an officeholder row
-- whose end_date is set; a sitting holder (end_date IS NULL) has no exit
-- reason yet. TERM_END is the ordinary case, the holder serving out the
-- leadership_term (V29); the rest cut the occupancy short.

CREATE TABLE office_exit_reason (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reason_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO office_exit_reason (reason_name, description)
VALUES ('TERM_END', 'Served the term to its end'),
       ('DEATH', 'Died in office'),
       ('RESIGNATION', 'Stood down of their own accord'),
       ('IMPEACHMENT', 'Removed by an impeachment process'),
       ('REMOVAL', 'Removed from the seat by another lawful process'),
       ('DISQUALIFICATION', 'Ceased to qualify to hold the seat');
