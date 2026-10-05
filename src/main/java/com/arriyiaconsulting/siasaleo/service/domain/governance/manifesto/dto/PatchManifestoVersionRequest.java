package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.json.bind.annotation.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.PatchFields;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** Tracks supplied properties so omitted values and explicit null remain distinct. */
public class PatchManifestoVersionRequest extends PatchFields {
    private String title;
    @JsonbProperty("title") public String getTitle() { return title; }
    @JsonbProperty("title") public void setTitle(String value) { mark("title"); title = value; }
    private String language;
    @JsonbProperty("language") public String getLanguage() { return language; }
    @JsonbProperty("language") public void setLanguage(String value) { mark("language"); language = value; }
    private String content;
    @JsonbProperty("content") public String getContent() { return content; }
    @JsonbProperty("content") public void setContent(String value) { mark("content"); content = value; }
}
