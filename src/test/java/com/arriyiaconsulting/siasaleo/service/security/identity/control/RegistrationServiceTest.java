package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import com.arriyiaconsulting.siasaleo.service.security.authorization.control.RoleAssignments;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.ResendCodeRequest;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.ResendResult;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.Identifier;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.VerificationCode;
import com.arriyiaconsulting.siasaleo.service.security.identity.mapping.IdentityMapper;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.UserAccountRepository;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.VerificationCodeRepository;
import jakarta.data.Limit;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    private static final String EMAIL = "jane@example.com";

    @Spy
    private IdentityMapper identityMapper = Mappers.getMapper(IdentityMapper.class);

    @Mock
    private UserAccountRepository accounts;

    @Mock
    private VerificationCodeRepository codes;

    @Mock
    private VerificationSender sender;

    @Mock
    private PasswordHasher passwords;

    @Mock
    private RoleAssignments roleAssignments;

    @InjectMocks
    private RegistrationService service;

    @Test
    void resendCodeDuringCooldownIsRateLimited() {
        UserAccount pending = new UserAccount(new Identifier.Email(EMAIL), "stored-hash");
        VerificationCode recent = new VerificationCode(
                pending, "hash", OffsetDateTime.now().plus(RegistrationService.CODE_TTL));
        when(accounts.findByIdentifier(new Identifier.Email(EMAIL))).thenReturn(Optional.of(pending));
        when(codes.findActiveByAccount(any(), any(Limit.class))).thenReturn(List.of(recent));

        ResendResult result = service.resendCode(new ResendCodeRequest(EMAIL, null));

        assertInstanceOf(ResendResult.RateLimited.class, result);
        verify(sender, never()).send(any(), any());
        verify(codes, never()).save(any());
    }

    @Test
    void resendCodeAfterCooldownSendsNewCode() throws Exception {
        UserAccount pending = new UserAccount(new Identifier.Email(EMAIL), "stored-hash");
        VerificationCode older = new VerificationCode(
                pending, "hash", OffsetDateTime.now().plus(RegistrationService.CODE_TTL));
        Field createdAt = VerificationCode.class.getDeclaredField("createdAt");
        createdAt.setAccessible(true);
        createdAt.set(older, OffsetDateTime.now().minus(RegistrationService.RESEND_COOLDOWN).minusSeconds(5));

        when(accounts.findByIdentifier(new Identifier.Email(EMAIL))).thenReturn(Optional.of(pending));
        when(codes.findActiveByAccount(any(), any(Limit.class))).thenReturn(List.of(older));

        ResendResult result = service.resendCode(new ResendCodeRequest(EMAIL, null));

        assertInstanceOf(ResendResult.CodeSent.class, result);
        verify(codes).save(any(VerificationCode.class));
        verify(sender).send(any(), any());
    }
}
