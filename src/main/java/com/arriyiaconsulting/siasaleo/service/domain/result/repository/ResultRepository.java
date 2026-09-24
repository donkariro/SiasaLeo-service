package com.arriyiaconsulting.siasaleo.service.domain.result.repository;

import com.arriyiaconsulting.siasaleo.service.domain.result.dto.ResultDtos.*;
import com.arriyiaconsulting.siasaleo.service.domain.result.dto.ResultStage;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.Coverage;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.PublicationStatus;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence.NativeQueries;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import static com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.SqlValues.*;

@ApplicationScoped
public class ResultRepository extends NativeQueries {
    private static final String COLUMNS="id,contest_id,geography_snapshot_id,scope_area_id,area_type_id,stage,source_reference,source_sha256,coverage,supersedes_id,status,cast(created_at as text),cast(published_at as text)";
    private PublicationDto map(Object[] r) {
        return new PublicationDto(number(r[0]),number(r[1]),number(r[2]),number(r[3]),number(r[4]),ResultStage.valueOf(text(r[5])),text(r[6]),text(r[7]),Coverage.valueOf(text(r[8])),number(r[9]),PublicationStatus.valueOf(text(r[10])),text(r[11]),text(r[12]));
    }
    public Optional<PublicationDto> find(long id,boolean lock) {
        return rows("SELECT "+COLUMNS+" FROM result_publication WHERE id=?1"+(lock?" FOR UPDATE":""),id).stream().findFirst().map(this::map);
    }
    public List<PublicationDto> list(Long contest,boolean admin,int offset,int size) {
        return rows("SELECT "+COLUMNS+" FROM result_publication WHERE "+(admin?"true":"status='PUBLISHED'")
            +" AND (?1=0 OR contest_id=?1) ORDER BY id DESC LIMIT ?2 OFFSET ?3",contest==null?0L:contest,size,offset).stream().map(this::map).toList();
    }
    public List<String> errors(long id) {
        return rows("SELECT error,1 FROM result_publication_errors(?1)",id).stream().map(r->text(r[0])).toList();
    }
}
