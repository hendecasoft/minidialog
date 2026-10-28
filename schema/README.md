# Schemas
Schemas for validation or inspection of MiniDialog documents, stratified by
spec version. Note that XML schemas are suboptimal for describing MiniDialog documents
as MiniDialog may contain MiniMessage, which is *not* an XML-compatible format (different
attribute system, supports unclosed and implicitly closed tags).

|    |                   XML DTD                    |
|:---|:----------------------------------------:|
| v1 | [`v1/minidialog.dtd`](v1/minidialog.dtd) |

## Usage
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE dialog PUBLIC "https://hendeca.moe/schema/v1/minidialog.dtd">
<dialog>
    <!-- ... -->
</dialog>
```
