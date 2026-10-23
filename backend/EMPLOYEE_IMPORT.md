# Employee bulk import

`POST /api/employees/import` accepts `multipart/form-data` with a file field named `file`. Only ADMIN and HR roles may use it.

Supported formats are `.csv` (UTF-8) and `.xlsx` (one worksheet). The first row is the header. Header names are case-insensitive and must use these field names:

| Required columns | Optional columns |
| --- | --- |
| `firstName`, `lastName`, `email`, `dateOfBirth`, `salary`, `joiningDate`, and one of `department` or `departmentId` | `phone`, `gender`, `designation`, `address` |

Dates must be ISO `yyyy-MM-dd`; salary must be a decimal number. `department` is trimmed and looked up by existing department name without case sensitivity. `departmentId` must identify an existing department. A row must provide one mapping, not both. Unknown departments are rejected by the existing employee creation workflow. New employees use that workflow's `ACTIVE` status default; status is not an import column.

The upload limit is 5 MB and the worksheet/CSV record limit is 1,000 rows after the header; blank rows count toward the limit. Missing/unsupported/duplicate headers, unsupported formats, malformed workbooks, and limit violations reject the file before any writes. A header-only file returns a successful zero-row summary. Blank rows are skipped and excluded from `totalRows`. For valid files, each non-empty row is validated and passed to the existing employee service independently. Successful rows commit independently and are not rolled back when another row is rejected. The response reports total, imported, rejected counts and each rejected row's 1-based source row number and reasons. An unexpected persistence failure can still interrupt processing after earlier rows committed; clients should use the returned summary and not assume batch atomicity.

Example response:

```json
{
  "totalRows": 2,
  "importedCount": 1,
  "rejectedCount": 1,
  "rejectedRows": [{"rowNumber": 3, "errors": ["email: duplicate email in this import file"]}]
}
```
