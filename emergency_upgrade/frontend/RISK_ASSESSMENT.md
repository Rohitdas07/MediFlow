# MediFlow Risk Assessment

The Verification & Token step now includes a deterministic triage/risk assessment with exactly three levels: LOW, MODERATE, HIGH.

## Inputs
- Symptoms and symptom severity
- Heart rate
- SpO₂
- Temperature
- Blood pressure
- Respiratory rate
- Age
- Available past medical history

## Behavior
- Critical warning symptoms or critically abnormal vitals force HIGH.
- Concerning symptoms/vitals produce MODERATE, especially when multiple findings occur together or relevant history/age increases concern.
- No major warning signs produces LOW when sufficient clinical information is available.
- Missing information is explicitly disclosed; if essentially no clinical data is available, the result is MODERATE because the assessment cannot safely establish low risk.

This feature is a triage/risk assessment, not a medical diagnosis. Clinical staff must verify the information and make the final decision.
