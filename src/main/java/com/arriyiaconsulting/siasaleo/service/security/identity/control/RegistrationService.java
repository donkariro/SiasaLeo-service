package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import com.arriyiaconsulting.siasaleo.service.security.authorization.control.RoleAssignments;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.RegisterRequest;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.RegistrationResult;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.ResendCodeRequest;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.ResendResult;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.UserAccountDto;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.VerificationResult;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.VerifyRequest;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.Identifier;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.VerificationCode;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.UserAccountRepository;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.VerificationCodeRepository;
import jakarta.data.Limit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Sign-up flow: register creates a PENDING_ACTIVATION account and sends a
 * one-time code to the chosen email/phone; verify proves control of that
 * contact point and activates the account. Every outcome is a variant of the
 * sealed result types in dto — this service never throws for business rules.
 */
@ApplicationScoped
public class RegistrationService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    // Package-private so RoutingVerificationSender can quote the real expiry
    // in the messages it composes.
    static final Duration CODE_TTL = Duration.ofMinutes(15);
    private static final int MAX_CODE_ATTEMPTS = 5;

    @Inject
    private UserAccountRepository accounts;

    @Inject
    private VerificationCodeRepository codes;

    @Inject
    private VerificationSender sender;

    @Inject
    private PasswordHasher passwords;

    @Inject
    private RoleAssignments roleAssignments;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public RegistrationResult register(RegisterRequest request) {
        return switch (Identifier.parseOneOf(request.email(), request.phone())) {
            case Identifier.Invalid(String reason) -> new RegistrationResult.InvalidIdentifier(reason);
            case Identifier.Parsed(Identifier identifier) -> registerWith(identifier, request);
        };
    }

    private RegistrationResult registerWith(Identifier identifier, RegisterRequest request) {
        if (!request.password().equals(request.passwordConfirmation())) {
            return new RegistrationResult.PasswordMismatch();
        }
        if (request.password().length() < MIN_PASSWORD_LENGTH) {
            return new RegistrationResult.WeakPassword(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        // The unique indexes on user_account email/phone are the real
        // guarantee; this check just turns the common case into a friendly
        // outcome.
        if (accounts.findByIdentifier(identifier).isPresent()) {
            return new RegistrationResult.IdentifierTaken();
        }

        UserAccount account = accounts.save(new UserAccount(
                identifier, passwords.generate(request.password())));
        issueCode(account, identifier);
        return new RegistrationResult.Registered(UserAccountDto.from(account));
    }

    @Transactional
    public VerificationResult verify(VerifyRequest request) {
        return switch (Identifier.parseOneOf(request.email(), request.phone())) {
            // An identifier that can't be normalized can't belong to any account.
            case Identifier.Invalid ignored -> new VerificationResult.AccountNotFound();
            case Identifier.Parsed(Identifier identifier) -> verifyWith(identifier, request.code());
        };
    }

    private VerificationResult verifyWith(Identifier identifier, String code) {
        Optional<UserAccount> found = accounts.findByIdentifier(identifier);
        if (found.isEmpty()) {
            return new VerificationResult.AccountNotFound();
        }
        UserAccount account = found.get();
        switch (account.getStatus()) {
            case ACTIVE -> {
                return new VerificationResult.AlreadyVerified();
            }
            case LOCKED, DISABLED -> {
                return new VerificationResult.AccountUnavailable();
            }
            case PENDING_ACTIVATION -> {
                // fall through to the code check below
            }
        }

        Optional<VerificationCode> latest = codes
                .findActiveByAccount(account.getId(), Limit.of(1)).stream().findFirst();
        if (latest.isEmpty() || latest.get().isExpired(OffsetDateTime.now())) {
            return new VerificationResult.CodeExpired();
        }
        VerificationCode active = latest.get();
        if (active.getAttempts() >= MAX_CODE_ATTEMPTS) {
            return new VerificationResult.TooManyAttempts();
        }
        if (!active.getCodeHash().equals(sha256(code))) {
            active.recordFailedAttempt();
            codes.save(active);
            return new VerificationResult.CodeInvalid(MAX_CODE_ATTEMPTS - active.getAttempts());
        }

        active.consume();
        codes.save(active);
        account.activate();
        UserAccount verified = accounts.save(account);
        // Verification is what makes an account usable, so it is also where
        // the baseline role is granted; without it every @RolesAllowed
        // endpoint would refuse a perfectly valid account.
        roleAssignments.grantDefaultRole(verified.getId());
        return new VerificationResult.Verified(UserAccountDto.from(verified));
    }

    @Transactional
    public ResendResult resendCode(ResendCodeRequest request) {
        return switch (Identifier.parseOneOf(request.email(), request.phone())) {
            case Identifier.Invalid ignored -> new ResendResult.AccountNotFound();
            case Identifier.Parsed(Identifier identifier) ->
                    accounts.findByIdentifier(identifier)
                            .map(account -> resendTo(account, identifier))
                            .orElseGet(ResendResult.AccountNotFound::new);
        };
    }

    private ResendResult resendTo(UserAccount account, Identifier identifier) {
        return switch (account.getStatus()) {
            case ACTIVE -> new ResendResult.AlreadyVerified();
            case LOCKED, DISABLED -> new ResendResult.AccountUnavailable();
            case PENDING_ACTIVATION -> {
                issueCode(account, identifier);
                yield new ResendResult.CodeSent();
            }
        };
    }

    private void issueCode(UserAccount account, Identifier identifier) {
        String code = "%06d".formatted(random.nextInt(1_000_000));
        codes.save(new VerificationCode(
                account, sha256(code), OffsetDateTime.now().plus(CODE_TTL)));
        sender.send(identifier, code);
    }

    // Plain SHA-256 is enough for 6-digit codes: they are short-lived and
    // attempt-limited, so brute force is bounded by MAX_CODE_ATTEMPTS, not
    // hash cost. Passwords use Pbkdf2PasswordHash instead.
    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
