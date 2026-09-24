package com.arriyiaconsulting.siasaleo.service.domain.election.control;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence.NativeQueries;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ContestJurisdictionService {
    @Inject private NativeQueries sql;
    public void assign(Contest contest,ElectionEvent event,Long office,Long area) {
        if(office==null || area==null || event.getGeographySnapshotId()==null) throw new IllegalArgumentException("Office, jurisdiction and election geography are required");
        if(!sql.exists("SELECT count(*) FROM electoral_area_snapshot a JOIN area_type t ON t.id=a.area_type_id JOIN office o ON o.id=?3 WHERE a.id=?1 AND a.snapshot_id=?2 AND t.name=CASE o.abbreviation WHEN 'PRES' THEN 'COUNTRY' WHEN 'DP' THEN 'COUNTRY' WHEN 'SEN' THEN 'COUNTY' WHEN 'WR' THEN 'COUNTY' WHEN 'GOV' THEN 'COUNTY' WHEN 'DG' THEN 'COUNTY' WHEN 'MP' THEN 'CONSTITUENCY' WHEN 'MCA' THEN 'WARD' END",area,event.getGeographySnapshotId(),office))
            throw new IllegalArgumentException("Jurisdiction must match the event geography and office level");
        if(contest.getSeatId()!=null && !sql.exists("SELECT count(*) FROM seat s JOIN electoral_area_correspondence c ON c.electoral_area_id=s.electoral_area_id WHERE s.id=?1 AND s.office_id=?2 AND c.area_snapshot_id=?3 AND c.reviewed_at IS NOT NULL",contest.getSeatId(),office,area))
            throw new IllegalArgumentException("Seat requires a reviewed correspondence with the jurisdiction");
        contest.assignJurisdiction(office,event.getGeographySnapshotId(),area);
    }
}
