-- V31: office_entry_reason — lookup table for how a person came to occupy a
-- seat: won at a general election, won the by-election that filled a vacancy,
-- was appointed, or succeeded the previous holder. It replaces the inline
-- VARCHAR CHECK on officeholder.acquisition (V14), which becomes a FK into
-- this table in V32.

CREATE TABLE office_entry_reason (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reason_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO office_entry_reason (reason_name, description)
VALUES ('GENERAL_ELECTION_WINNER', 'Won the seat at a general election'),
       ('BY_ELECTION_WINNER', 'Won the by-election held to fill a vacancy'),
       ('APPOINTMENT', 'Placed in the seat by appointment rather than a ballot'),
       ('SUCCESSION', 'Took the seat on succeeding the previous holder');
