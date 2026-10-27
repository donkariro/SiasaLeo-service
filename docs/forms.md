# Forms

The forms domain collects structured information whose questions vary by client
and change over time, without a redeploy. It owns definitions, versions, draft
answers and immutable submissions. Consuming domains (nominations first) own
every business decision that follows from the answers.

Status: v1 implemented in V59. Administrators author definitions as JSON; a
party-facing builder is deferred until pilots show which edits parties need.

## Model

| Concept | Meaning |
| --- | --- |
| Tenant (`forms.tenant`) | An owner of forms and answers. `tenant_type` + opaque `external_ref`; consumers map their records onto it (a political party is `POLITICAL_PARTY` / its id) |
| Definition | A stable form identity with a `purpose_key` (e.g. `nomination.application`). Platform cores have no tenant; tenant definitions extend a core |
| Version | Authored `source`, compiled `resolved`, status DRAFT → PUBLISHED → RETIRED. Published versions are immutable and carry a SHA-256 `checksum` of the canonical resolved JSON |
| Instance | One use of a form: draft answers pinned to one published version |
| Submission | An immutable revision of the answers, with the version checksum and a consumer-supplied context snapshot |
| Submission evidence | The exact sealed `digital_asset_version` submitted for a document field |
| Declaration acceptance | The exact declaration wording accepted with a submission |

Relational columns hold ownership, versions, actors and workflow. JSONB holds the
definition tree and the answers. The tables live in the `forms` schema: other
domains may reference `forms.*`; nothing in `forms` references a consuming domain.

Extension is one level deep: platform core → tenant extension. The database
rejects tenant versions without a base, cores with a base, bases that are not a
platform core of the same purpose, and publishing against an unpublished core.

## Definition format

Keys are letters, digits and underscores. The compiler qualifies them: core keys
become `core.<key>`, extension keys `ext.<key>`, so an extension can never
collide with or redefine a core field. Unknown attributes are rejected, which
catches typos such as `requred`.

Core source:

```json
{
  "format": 1,
  "title": "Nomination application",
  "sections": [
    { "key": "contact", "title": "Contact", "fields": [
      { "key": "phone", "type": "text", "label": "Phone", "required": true, "maxLength": 20 },
      { "key": "publicOfficer", "type": "boolean", "label": "Are you a public officer?", "required": true },
      { "key": "resignation", "type": "document", "label": "Resignation letter", "assetType": "DOCUMENT",
        "visibleWhen": { "field": "publicOfficer", "equals": true },
        "requiredWhen": { "field": "publicOfficer", "equals": true } } ] },
    { "key": "additional", "title": "Party questions", "fields": [] }
  ],
  "declarations": [ { "key": "truth", "text": "The information I have given is true." } ],
  "extensionPolicy": { "addFieldsTo": ["additional"], "helpEditable": ["phone"],
                       "addSections": false, "addDeclarations": true }
}
```

Extension source, compiled against an exact published core version:

```json
{
  "format": 1,
  "help": { "phone": "Use a number the nomination office can reach." },
  "addFields": [ { "section": "additional", "fields": [
    { "key": "disciplinary", "type": "boolean", "label": "Any previous disciplinary proceedings?", "required": true },
    { "key": "details", "type": "textarea", "label": "Describe them",
      "visibleWhen": { "field": "disciplinary", "equals": true },
      "requiredWhen": { "field": "disciplinary", "equals": true } } ] } ],
  "addDeclarations": [ { "key": "code", "text": "I accept the party code of conduct." } ]
}
```

An extension can add fields only to sections listed in `addFieldsTo`, change help
text only on `helpEditable` fields, and add sections or declarations only when
allowed. It cannot remove a core field or change its meaning, key or type.
Extension conditions refer to core fields as `core.<key>`.

| Type | Attributes | Answer |
| --- | --- | --- |
| text, textarea | minLength, maxLength (defaults 255 / 5000, at most 20000) | string |
| number | min, max, integer | number |
| date | min, max (ISO dates) | `yyyy-mm-dd` |
| boolean | — | `true` / `false`; `null` or absent means unanswered |
| choice | options `[{value,label}]` | an option value |
| multichoice | options, minItems, maxItems | array of distinct option values |
| document | assetType, multiple | asset version ID, or an array when `multiple` |

Every field accepts `label` (required), `help`, `required`, `visibleWhen` and
`requiredWhen`. Sections accept `visibleWhen` and `description`.

Conditions are `{"field":k,"equals":v}`, `{"field":k,"in":[...]}`,
`{"field":k,"contains":v}` (multichoice), combined with `all`, `any` and `not`. A
condition may only refer to fields that come earlier in the form, which rules out
cycles. The Angular renderer must implement the same semantics; definitions never
contain executable code.

## Answers and validation

Answers are grouped by origin: `{"core":{"phone":"..."},"ext":{"details":"..."}}`.
The validator visits fields in form order and drops answers to fields that are
not applicable, so hiding a control in the browser is not the only safeguard.

| Operation | Rules |
| --- | --- |
| Save draft | Types, limits and unknown fields are checked; required answers are not |
| Submit | Also required and conditionally required answers, every declaration accepted, and each document a sealed version the submitter can read |

Invalid definitions and answers return 400 with every problem:
`{"error": "...", "problems": [{"field": "core.phone", "message": "..."}]}`.

Submitting takes the instance's `row_version`. Repeating a submit for the same
draft returns the revision it already created; a stale `row_version` is a 409.
Corrections are saved and submitted again as the next revision; earlier
revisions are never altered.

## Authoring API

Routes are relative to `/api/forms` and require ADMINISTRATOR.

| Method and route | Behavior |
| --- | --- |
| GET /definitions?purpose= | List definitions |
| POST /definitions | code, purpose_key, title; tenant_type + tenant_ref for a tenant form |
| GET /definitions/{id} | One definition |
| GET /definitions/{id}/versions | Version history, newest first |
| POST /definitions/{id}/versions | Draft from `source`; tenant forms give `base_version_id` |
| GET /versions/{id} | Source and resolved definition |
| PUT /versions/{id} | Replace a draft's source; requires row_version |
| POST /versions/{id}/preview | Validate sample `answers`; `submit: true` applies submission rules |
| POST /versions/{id}/publish | Recompile and freeze; requires row_version |
| POST /versions/{id}/retire | Stop new use; existing instances keep it |

Consuming domains use `FormInstanceService` (open, read, save, submit,
submissions). It performs no authorization: the consumer authorizes through its
own record first.

## Deferred

- A party-facing form builder on the same publishing operations.
- Moving drafts to a newer form version.
- Reusable fragments and multi-level extension.
- A derived `forms.compiled_field` catalogue for SQL queries across definitions.
- Application-scoped access to submitted documents for reviewers (see
  [education evidence and party applications](education-evidence-and-party-applications.md), phase 6).
