package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control;

import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordNotFoundException;
import jakarta.data.page.PageRequest;

/** Pure workflow rules shared by the governance controls. */
public final class GovernanceRules {
    private GovernanceRules() {}
    public static void requireDraft(PublicationStatus status) {
        if (status != PublicationStatus.DRAFT) throw new IllegalArgumentException("Published content requires a new revision");
    }
    public static void requirePublic(PublicationStatus status) {
        if (status == PublicationStatus.DRAFT) throw new RecordNotFoundException();
    }
    public static PublicationStatus transition(PublicationStatus current, boolean withdraw) {
        if (withdraw) {
            if (current != PublicationStatus.PUBLISHED) throw new IllegalArgumentException("Only published content can be withdrawn");
            return PublicationStatus.WITHDRAWN;
        }
        requireDraft(current);
        return PublicationStatus.PUBLISHED;
    }
    public static int next(Integer current) { return current == null ? 1 : Math.addExact(current, 1); }
    public static PageRequest page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Use page >= 0 and size 1..100");
        return PageRequest.ofPage(page + 1L).size(size).withoutTotal();
    }
}
