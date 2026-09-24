package com.arriyiaconsulting.siasaleo.service.domain.voter.repository;

import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegisterDtos.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.Coverage;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.PublicationStatus;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence.NativeQueries;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import static com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.SqlValues.*;

@ApplicationScoped
public class VoterRegisterRepository extends NativeQueries {
    private static final String COLUMNS="id,geography_snapshot_id,scope_area_id,area_type_id,source_reference,source_sha256,coverage,supersedes_id,status,cast(created_at as text),cast(published_at as text)";
    private RegisterDto map(Object[] r) {
        return new RegisterDto(number(r[0]),number(r[1]),number(r[2]),number(r[3]),text(r[4]),text(r[5]),Coverage.valueOf(text(r[6])),number(r[7]),PublicationStatus.valueOf(text(r[8])),text(r[9]),text(r[10]));
    }
    public Optional<RegisterDto> find(long id,boolean lock) {
        return rows("SELECT "+COLUMNS+" FROM voter_register WHERE id=?1"+(lock?" FOR UPDATE":""),id).stream().findFirst().map(this::map);
    }
    public List<RegisterDto> list(boolean admin,int offset,int size) {
        return rows("SELECT "+COLUMNS+" FROM voter_register WHERE "+(admin?"true":"status='PUBLISHED'")+" ORDER BY id DESC LIMIT ?1 OFFSET ?2",size,offset).stream().map(this::map).toList();
    }
    public List<String> errors(long id) {
        return rows("SELECT error,1 FROM voter_register_errors(?1)",id).stream().map(r->text(r[0])).toList();
    }
}
