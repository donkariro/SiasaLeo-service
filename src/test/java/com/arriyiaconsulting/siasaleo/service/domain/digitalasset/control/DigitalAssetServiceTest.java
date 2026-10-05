package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.CreateDigitalAssetRequest;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.StoredFileDto;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.DigitalAsset;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.DigitalAssetVersion;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.mapping.DigitalAssetMapper;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository.DigitalAssetRepository;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository.DigitalAssetVersionRepository;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository.StoredFileRepository;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordAccessDeniedException;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordNotFoundException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigitalAssetServiceTest {

    @Spy
    private DigitalAssetMapper mapper = Mappers.getMapper(DigitalAssetMapper.class);

    @Mock
    private DigitalAssetRepository assets;

    @Mock
    private DigitalAssetVersionRepository versions;

    @Mock
    private StoredFileRepository files;

    @InjectMocks
    private DigitalAssetService service;

    @Test
    void anotherAccountCannotSealAsset() {
        version(8L, 4L, null);
        assetCreatedBy(4L, 1L);

        assertThrows(RecordAccessDeniedException.class, () -> service.seal(8L, 2L));
        verify(versions, never()).update(any());
    }

    @Test
    void uploaderIdentityComesFromTheCallingAccount() {
        when(assets.insert(any())).then(returnsFirstArg());

        service.create(new CreateDigitalAssetRequest("Report", null, "REPORT"), 1L);

        ArgumentCaptor<DigitalAsset> inserted = ArgumentCaptor.forClass(DigitalAsset.class);
        verify(assets).insert(inserted.capture());
        assertEquals(1L, inserted.getValue().getCreatedByAccountId());
    }

    @Test
    void publicReadHidesUnsealedVersions() {
        version(8L, 4L, null);
        assertThrows(RecordNotFoundException.class, () -> service.read(8L, null));
        verify(files, never()).forVersion(anyLong());
    }

    @Test
    void publicReadShowsSealedVersions() {
        version(8L, 4L, OffsetDateTime.now());
        assetCreatedBy(4L, 1L);
        when(files.forVersion(8L)).thenReturn(List.of());

        assertNotNull(service.read(8L, null));
    }

    @Test
    void storedFilesDoNotExposeStorageKeys() {
        assertTrue(Arrays.stream(StoredFileDto.class.getRecordComponents())
                .noneMatch(c -> c.getName().toLowerCase(Locale.ROOT).contains("storage")));
    }

    private void version(long id, long assetId, OffsetDateTime sealedAt) {
        DigitalAssetVersion version = new DigitalAssetVersion();
        version.setAssetId(assetId);
        version.setSealedAt(sealedAt);
        when(versions.findById(id)).thenReturn(Optional.of(version));
    }

    private void assetCreatedBy(long id, long accountId) {
        DigitalAsset asset = new DigitalAsset();
        asset.setCreatedByAccountId(accountId);
        when(assets.findById(id)).thenReturn(Optional.of(asset));
    }
}
