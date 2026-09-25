-- Idempotent legacy migration for permanent MediFlow Patient IDs.
-- Safe to run after 01_core_persistence.sql.

USE mediflow_ai;

-- Backfill patients created before patient_code was introduced.
UPDATE patients
SET patient_code = CONCAT(
    'MFP-',
    YEAR(COALESCE(created_at, CURRENT_DATE)),
    '-',
    UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 8))
)
WHERE patient_code IS NULL OR TRIM(patient_code) = '';

-- Convert legacy OPD rows that stored the numeric patients.id into the
-- permanent patient_code. Rows with no reliable patient link are left NULL.
UPDATE opd_tokens o
JOIN patients p ON o.patient_code REGEXP '^[0-9]+$'
              AND CAST(o.patient_code AS UNSIGNED) = p.id
SET o.patient_code = p.patient_code;
