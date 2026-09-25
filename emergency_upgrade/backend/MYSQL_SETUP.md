# MediFlow Backend + MySQL Setup

## 1. Start MySQL
Ensure MySQL Server is running on localhost:3306.

Default development settings:
- Database: `mediflow_ai`
- User: `root`
- Password: `12345`

For a different MySQL installation, set:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## 2. Optional manual database preparation
Run `src/main/resources/mysql/01_core_persistence.sql` in MySQL Workbench.

Spring Boot is also configured with `createDatabaseIfNotExist=true` and Hibernate `ddl-auto=update`, so a normal local run can create/update the schema automatically.

## 3. Start Spring Boot
From the `backend` folder:

```powershell
mvn clean spring-boot:run
```

## 4. Verify MySQL connection
Open:

`http://localhost:8080/api/health`

Expected:

```json
{"status":"UP","mysql":"UP"}
```

## 5. Core persistent data
- `patients`: unique Patient ID (`patient_code`)
- `hospital_staff`: Admin-created Doctor/Staff IDs and BCrypt PIN hashes
- `doctors`: doctor directory used by appointment workflows
- `opd_tokens`: issued daily OPD tokens
- `opd_daily_sequences`: atomic daily counters preventing duplicate OPD tokens

Do not store plain-text PINs in MySQL. Admin-created PINs are stored as BCrypt hashes.

## Permanent Patient ID migration

The application automatically runs an idempotent startup migration. It:
- assigns `MFP-YYYY-XXXXXXXX` codes to legacy patients whose `patient_code` is NULL/blank;
- converts legacy numeric `opd_tokens.patient_code` values (for example `1`) to the matching patient's permanent code;
- never guesses the patient for an OPD row whose patient link is genuinely unknown.

For manual MySQL Workbench migration, run `src/main/resources/mysql/02_patient_id_migration.sql` after selecting `mediflow_ai`.
