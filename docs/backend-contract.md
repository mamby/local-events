# Local Events backend contract — v1

Build your own backend for the native clients using any language, framework, database, hosting provider, or event sources. This public contract is independent of the private reference service. Configure the Android build with `LOCAL_EVENTS_API_<FLAVOR>` as described in [the Android guide](../src/android/README.md).

[OpenAPI document](api/openapi.v1.json) · [Complete JSON feed example](api/feed.example.json)

The OpenAPI document is generated from the reference endpoint schemas, with annotations for runtime validation, repeated query parameters, rate limiting, and the session header. This guide adds semantics that schemas cannot express. The reference feed currently synthesizes demo events; production discovery, ingestion, moderation, accounts, and administration are not implemented or required as private implementation dependencies.

## Transport and compatibility

- Use an absolute HTTPS base URL with a trailing slash and a device-trusted certificate. Client paths below are relative to that URL. The Android application does not permit cleartext HTTP by default.
- Responses are UTF-8 JSON with camelCase properties. POST interactions use `Content-Type: application/json`. Session creation has no request body.
- Event IDs are stable UUID strings. Use ISO 8601 timestamps with offsets; dates in filters use `YYYY-MM-DD`.
- Public feed/search and session creation require no account login, bearer token, or API key. Interaction recording uses the anonymous telemetry session header described below.
- Preserve `/v1` routes, field meanings, and enum values. Adding optional fields is compatible: the client ignores unknown JSON fields. Removing required data or changing enum representations requires a coordinated new API version.
- Clients should treat cursors, scope IDs, and category values as opaque data, except the reserved scopes `world`, `online`, and `tv`. Do not require access to private source, schemas, credentials, or event providers.

## Endpoints

| Method and path | Success | Purpose |
| --- | --- | --- |
| `GET /health` | `200 {"status":"Healthy"}` | Reference-service liveness; not called by the Android client |
| `GET /v1/feed` | `200 FeedPageResponse` | Filtered, paginated event feed |
| `GET /v1/feed/attendance-scopes/search` | `200 FeedAttendanceScope[]` | Geographic, online, and television scopes |
| `GET /v1/feed/locations/search` | `200 FeedLocationScope[]` | Legacy geographic-only search |
| `GET /v1/feed/categories/search` | `200 FeedCategoryOption[]` | Stable category values with localized labels |
| `GET /v1/feed/terms/search` | `200 FeedTermOption[]` | Text suggestions |
| `POST /v1/telemetry/sessions` | `200 {"sessionToken":"..."}` | Create an anonymous session |
| `POST /v1/telemetry/interactions` | `204`, empty body | Record an idempotent interaction |

The current Android client uses all feed endpoints except legacy location search. For full v1 compatibility, implement every route in the OpenAPI document. A backend that does not retain analytics can issue opaque session tokens and accept valid interactions without storing them; it should still honor the same status and validation contract.

## Feed request

Always send `page` and `pageSize`. Despite property initializers in the reference implementation, its HTTP binder requires both parameters. Search endpoints likewise require `limit`.

| Query parameter | Meaning |
| --- | --- |
| `page` | Required zero-based integer, at least 0 |
| `pageSize` | Required positive integer; the client normally uses 10 |
| `lang` | Requested language tag; omitted/unsupported values resolve to English |
| `terms` | Repeated search terms: `terms=music&terms=outdoor` |
| `attendanceScopeIds` | Repeated scope IDs from search; omission means unrestricted attendance |
| `locations` | Repeated free-text location names |
| `category` | Stable category value returned by category search |
| `accessibility` | Free-text accessibility filter |
| `accessibleForAge` | Integer 0–120; admission eligibility, not recommended age |
| `dateFrom`, `dateTo` | Inclusive occurrence start-date bounds, `YYYY-MM-DD` |
| `priceFilter` | `Any`, `Free`, or `Paid`, case-insensitive; omitted means `Any` |
| `knownCursor` | Opaque cursor from an earlier response for the same query/language |

The reference endpoint also accepts `text`, `location`, `attendanceScopeId`, and `locationScopeId` as legacy aliases, combined with their plural counterparts. Arrays are optional and encoded as repeated query keys, not comma-separated strings or JSON.

```http
GET /v1/feed?lang=en&page=0&pageSize=10&attendanceScopeIds=online&terms=music&priceFilter=Free
Accept: application/json
```

Filtering semantics in the reference service:

- Different filter dimensions combine with AND. Multiple `terms` combine with OR; words within each term must all match. Matching is case-insensitive.
- Attendance scopes and free-text locations combine with OR. Geographic scopes include descendant locations. `world` covers physical attendance; `online` and `tv` cover their respective attendance types.
- Category matching uses stable values and can also match localized category text. Accessibility searches the available localized accessibility text.
- Date filtering matches any occurrence whose start date is in the inclusive range, using the timestamp's own offset. The displayed `startDate` becomes the earliest matching occurrence. Missing one bound is allowed.
- `Free` and `Paid` match any corresponding price option; an event with both matches either filter.
- For `accessibleForAge`, an unknown age restriction is excluded; an explicit unrestricted event or a minimum age not exceeding the requested age matches. Recommended ages do not control admission.

The reference API rejects negative pages, nonpositive page sizes, unsupported attendance scopes, invalid price filter strings, and ages outside 0–120. Invalid scalar/date syntax also returns 400. It currently has no explicit page-size cap and no validation error specifically for reversed date bounds; an implementation should document operational limits without silently changing requested pagination.

## Feed response and event data

`FeedPageResponse` contains:

- `items`: event array; return an empty array when no results remain. The client uses page-based loading; do not substitute a cursor-based paging envelope.
- `generatedAt`: response generation timestamp.
- `refreshCursor`: nullable opaque refresh marker, not a next-page cursor.
- `newSinceCount`: nonnegative count of returned-page items newer than the supplied `knownCursor`. The reference returns 0 for absent/unrecognized cursors and uses the maximum item update marker on the page as its response cursor. It is not a global unseen count.

Use the OpenAPI schemas for the full property inventory. Each event supplies a stable `id`, media, localized title/category/description, `categoryValue`, source, location context, timestamps, occurrence and price arrays, and a price label. Optional detail/accessibility/age data may be null. `sequence` is an ordering hint, not identity. Favorites are maintained locally by the client; anonymous telemetry does not synchronize favorites between devices.

| Structure | Rules |
| --- | --- |
| `LocalizedText` | `{"default":"Music","translations":{"en":"Music","fr":"Musique"}}`; include `default` and a translations object, even if empty |
| `Media` | `type` is **0 (Image) or 1 (Video)** in the reference API; `url` is required. Optional `thumbnailUrl`, `audioUrl`, and `durationSeconds`. Android also accepts `Image`/`Video` strings, but numeric values match this OpenAPI snapshot |
| `EventAttendanceOption` | `type`: `Physical`, `Online`, or `Television`; optional localized `displayName`/`address`, `locationScopeId`, and `isPrimary` |
| `EventOccurrence` | `startsAt`, optional `endsAt` strictly later than the start; at least one occurrence per event |
| `EventPriceOption` | `type`: `Free` or `Paid`. Free has null amount/currency; paid has positive integer `amountMinorUnits` and currency code. All paid options for one event use the same currency. At least one option per event |
| `AgeRestriction` | Null object means unknown; `{"minimumRequiredAge":null}` means explicitly unrestricted; otherwise 1–120 |
| `RecommendedAge` | `minimumAge` 0–120; nullable `maximumAge`, at least the minimum and at most 120. Recommendation must not start below the admission minimum |
| `EventSource` | `name`, nullable URL `link`, and display text `type` |

Minor units follow the currency's fractional digits: EUR 1800 means EUR 18.00. Media URLs must be reachable by the client; there is no media upload or signed-URL negotiation endpoint in v1. Backend-computed media convenience fields in the generated schema are safe for the client to ignore.

## Search and localization

Every search route accepts `lang`, `text`, and a required positive `limit` (the client normally sends 8). The reference caps requested search limits at 20. Empty/whitespace text returns no location, attendance, or term suggestions; category search can return its available categories without text.

Scope objects include `id`, localized `name`, optional `parentId`, localized `displayPath`, `isSearchable`, and `aliases`. Attendance scopes also include `type`: `Geographic`, `Online`, or `Television`. Return stable IDs that your feed endpoint accepts; do not expose nonsearchable grouping nodes as selectable results. Geographic catalogs and event sources are implementation choices, not a requirement to copy the demo's French/Malian data.

Category and term suggestions are `{ "value": "...", "label": "..." }`. Category values remain stable across locales; labels are localized. Term values are the text sent back to feed search.

The current client supports `en`, `fr`, `ar`, `es`, `pt`, `de`, `zh-Hans`, `hi`, `id`, `ja`, `ko`, `it`, `tr`, `ru`, `nl`, `pl`, `vi`, and `th`. Regional tags normalize to supported neutral languages; Indonesian `in` maps to `id`, and Simplified Chinese uses `zh-Hans`/CN/SG forms. Unsupported forms fall back to English. For event text, the client resolves exact tag, neutral tag, English, then `default`.

## Anonymous telemetry

Create a session with an empty-body `POST /v1/telemetry/sessions`. Return a nonempty opaque `sessionToken` with 200. The client stores it locally; there is no fixed expiry or refresh-token protocol in v1.

```http
POST /v1/telemetry/interactions
Content-Type: application/json
X-Fralov-Telemetry-Session: <sessionToken>

{"eventId":"123e4567-e89b-12d3-a456-426614174000","action":"view"}
```

Keep the historical **`X-Fralov-Telemetry-Session`** spelling for compatibility. Valid actions are `view`, `favorite_added`, `favorite_removed`, and `share_intent`. A share intent records opening the share action, not proof that a share completed.

- Missing, blank, unknown, or revoked session: 401. The client creates a replacement session and retries once.
- Invalid event UUID or action: 400 validation problem. No event-existence lookup is required by the current reference service.
- Accepted or already-counted interaction: 204 with no body.
- Rate-limited: 429. The reference limits telemetry requests to 300 per IP per minute and interactions to 120 per session per minute, with no queue. These are implementation policy values, not client constants; `Retry-After` is not guaranteed.

Retries must be safe. Deduplicate views and share intents by session, event, action, and UTC day. Favorite actions set state: repeated adds/removes must not repeatedly increment/decrement counts. The client queues failed deliveries locally; 400 is dropped as permanent, 429/5xx/network failures remain eligible for later retry. Session tokens represent anonymous analytics identity, not authenticated user accounts.

## Errors and implementation checklist

Validation failures use 400 and an `application/problem+json` object with an `errors` map from field name to message array. The reference uses names such as `Page`, `PageSize`, `EventId`, and `Action`; clients should not depend on English message wording. Other 400 binding failures may use a general Problem Details response instead of an `errors` map. Do not expose development exception details in a production implementation.

Before connecting a custom backend, check: all required feed fields deserialize; UUIDs remain stable; all search values round-trip to feed filters; repeated parameters work; empty results and page advancement work; localized text has fallbacks; occurrence/price enums match; cursors remain opaque; and telemetry retries are idempotent. The reference service's SQL Server tables, migration history, implementation namespaces, and hosting topology are not part of this public contract.

## Maintaining this contract

The private service uses ASP.NET Core's built-in OpenAPI endpoint in development. Its `tools/Export-PublicContract.ps1` exports endpoint schemas here and annotates runtime constraints. Review this guide and the JSON example alongside every export. Keep the public document self-contained; public contributors can implement and validate a backend without running that export tool or accessing the private repository.
