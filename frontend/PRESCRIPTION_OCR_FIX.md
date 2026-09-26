# MediFlow – Prescription OCR Fix

## Fixed issues
- Corrects common OCR spelling errors such as `Amoxiciillin` -> `Amoxicillin` and `Cetrizine` -> `Cetirizine`.
- Prevents an unrelated AI medicine name such as `Cobimzbue` from replacing a medicine identified by local OCR.
- Parses multi-line handwritten prescription rows so medicine name, strength, dosage form, frequency, duration and food instruction can be on separate lines.
- Correctly extracts the supplied sample as:
  - Paracetamol 500 mg — 1 tablet — Three times daily (1-1-1) — 3 days — After food
  - Cetirizine 10 mg — 1 tablet — At bedtime (0-0-1) — 5 days — At bedtime
  - Amoxicillin 500 mg — 1 capsule — Three times daily (1-1-1) — 5 days — After food
- Gemini remains available for multimodal verification, but local OCR is used as the factual anchor whenever it has a usable value.

## Important
The original `.env` file was intentionally not included in this redistributed ZIP. Use `.env.example` / Google AI Studio Secrets for runtime credentials.

## Upload reliability fix
- The kiosk upload previously called `/api/intake/ocr-document`, but this Spring Boot project does not expose that controller. A 404 entered the catch block, where `localOcrText` was out of scope, causing a second `ReferenceError` and preventing the uploaded document from being added to the UI.
- `localOcrText` is now scoped across the complete processing flow.
- Server OCR is optional: HTTP 404/network failures fall back to browser OCR instead of failing the upload.
- The original uploaded image is saved and displayed even when OCR cannot read it.
- Image preprocessing (contrast/grayscale enhancement and resizing) is applied before local Tesseract OCR.
- The file picker now accepts image formats explicitly and shows a visible error message for unsupported files.
- Selecting the same image again works because the file input is reset after each selection.
