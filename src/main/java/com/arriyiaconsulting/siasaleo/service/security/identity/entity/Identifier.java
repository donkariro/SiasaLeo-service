package com.arriyiaconsulting.siasaleo.service.security.identity.entity;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The contact point an account registers with: exactly one of an email
 * address or a phone number. Values only exist in normalized form (emails
 * lower-cased, phones in E.164), so two spellings of the same contact point
 * always compare equal — the unique index on user_account.identifier relies
 * on this.
 */
public sealed interface Identifier {

    String value();

    IdentifierType type();

    record Email(String value) implements Identifier {
        @Override
        public IdentifierType type() {
            return IdentifierType.EMAIL;
        }
    }

    record Phone(String value) implements Identifier {
        @Override
        public IdentifierType type() {
            return IdentifierType.PHONE;
        }
    }

    /** Outcome of normalizing raw user input into an Identifier. */
    sealed interface ParseOutcome {
    }

    record Parsed(Identifier identifier) implements ParseOutcome {
    }

    record Invalid(String reason) implements ParseOutcome {
    }

    Pattern EMAIL_PATTERN = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    Pattern E164_PATTERN = Pattern.compile("\\+[1-9][0-9]{7,14}");

    static ParseOutcome parseEmail(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 255 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            return new Invalid("Not a valid email address");
        }
        return new Parsed(new Email(normalized));
    }

    static ParseOutcome parsePhone(String raw) {
        String normalized = raw.replaceAll("[\\s().-]", "");
        // National format (07xx/01xx…) is assumed to be a Kenyan number;
        // anything else must already be in international E.164 form.
        if (normalized.matches("0[0-9]{9}")) {
            normalized = "+254" + normalized.substring(1);
        }
        if (!E164_PATTERN.matcher(normalized).matches()) {
            return new Invalid("Not a valid phone number; use international format, e.g. +254712345678");
        }
        return new Parsed(new Phone(normalized));
    }
}
