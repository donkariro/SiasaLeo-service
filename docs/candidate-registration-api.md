# Candidate self-registration

Both endpoints require a signed-in USER or ADMINISTRATOR. No administrator role
is needed for ordinary users. The account comes from the authentication token;
clients cannot choose a personId or accountId.

## Before rendering the form

Call `GET /api/candidacies/me/registration-form` and wait for the result before
rendering editable fields. An existing active voter receives:

```json
{
  "registeredVoter": true,
  "voterFieldsReadOnly": true,
  "profile": {
    "firstName": "Amina",
    "lastName": "Odhiambo",
    "dateOfBirth": "1994-02-08",
    "gender": "FEMALE"
  },
  "voterRegistration": {
    "id": 9,
    "personId": 5,
    "firstName": "Amina",
    "lastName": "Odhiambo",
    "registrationCenterId": 42,
    "registrationCenterName": "Kibra Primary School",
    "registrationDate": "2022-01-10",
    "status": "ACTIVE"
  }
}
```

Populate the shared profile and voter fields and make them read-only when
`voterFieldsReadOnly` is true. Contest and political party fields remain editable.

For a non-voter, both flags are false and `voterRegistration` is null. The
`profile` is null for a new account, or contains the account's existing person
information. Show all shared fields as editable. The lookup creates no records.
A retired voter registration does not count as an active registration.
If the lookup fails, show a retry/error state rather than assuming non-voter status.

## Submit the form

`POST /api/candidacies`

An existing voter only needs to submit candidate-specific details:

```json
{ "contestId": 7, "politicalPartyId": null }
```

For a non-voter, submit the combined form:

```json
{
  "profile": {
    "firstName": "Amina",
    "lastName": "Odhiambo",
    "dateOfBirth": "1994-02-08",
    "gender": "FEMALE"
  },
  "voterRegistration": {
    "registrationCenterId": 42,
    "registrationDate": "2026-05-20"
  },
  "contestId": 7,
  "politicalPartyId": null
}
```

These profile fields are the same ProfileDetails contract as the voter form.
The voter date is optional and defaults to today. Select centres using
`GET /api/electoral-areas?type=REGISTRATION_CENTER&page=0&size=100` or the electoral
area hierarchy. Example IDs are illustrative.

The service rechecks active voter status on submission. For an active voter,
submitted profile/voter values cannot overwrite the stored shared details.
For a non-voter, edited profile values are saved and a new active registration is
created for that same person. A first-time user's person is also created and linked
to their account. All these writes and the candidacy are one transaction.
If registration has ceased to be active since loading the form, missing voter
details cause a 400; reload the form so the user can complete the newly editable fields.

The centre must exist and be a REGISTRATION_CENTER. Future registration dates
are rejected. Existing voter rules require age 18 on the registration date when
a birth date is provided; unknown birth dates remain allowed. Duplicate candidacy
for the same person and contest is rejected. Supplied fields still undergo request
validation even when their stored values will be reused.

Success returns 201 Created, a Location header, and the candidacy DTO with status
EXPRESSED_INTEREST. Invalid input returns 400; unauthenticated requests return 401.
No database migration is needed for this registration change.

This repository provides the backend contract; the frontend must call the form
endpoint and apply the read-only flag before displaying the registration form.
The old arbitrary-person registration request (`personId` or `person`) is replaced
by account-scoped `profile` and `voterRegistration` fields.
