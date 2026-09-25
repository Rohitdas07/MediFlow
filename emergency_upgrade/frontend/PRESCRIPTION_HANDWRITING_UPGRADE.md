# MediFlow Prescription Handwriting Upgrade

## Recognition pipeline

1. Image quality validation and preprocessing.
2. Handwriting-aware local OCR fallback using multiple Tesseract page-segmentation passes (structured rows + sparse text).
3. Server multimodal prescription recognition is requested with `recognitionMode: handwriting_prescription` and `handwritingAware: true`.
4. Medicine candidates are normalized against MediFlow's curated medicine dictionary.
5. Fuzzy matching returns a similarity score and does not silently convert ambiguous text into a medicine.
6. Low-confidence or ambiguous medicine names are shown as **Unclear — Verify Medicine**.
7. A possible dictionary match is shown to the user for review.
8. Saving is blocked until every flagged medicine is explicitly verified or corrected.
9. The original prescription image and raw OCR transcription remain available for traceability.

## Safety behavior

- High-confidence exact matches can be structured automatically.
- Conservative fuzzy matches are accepted only when the similarity is strong enough.
- Ambiguous matches are never presented as confirmed medicines.
- This feature is an extraction/verification aid, not an autonomous prescribing system.
