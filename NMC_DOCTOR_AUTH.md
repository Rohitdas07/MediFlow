# MediFlow — NMC Doctor Authentication

## Implemented flow

1. HIS Master Admin signs in and receives a JWT-protected admin session.
2. Admin provisions a clinical account with an internal MediFlow Doctor ID and initial PIN.
3. For doctors / medical officers, the admin records the NMC/IMR registration number and State Medical Council.
4. The admin opens the official NMC Indian Medical Register and checks the doctor registration externally.
5. The admin records the name/qualification shown in the NMC/IMR result and confirms verification inside MediFlow.
6. MediFlow stores the verification status and timestamp.
7. Doctor / Medical Officer login is blocked until `nmcVerified=true`.
8. If the registration number is changed, MediFlow automatically clears NMC verification and the doctor must be re-verified.
9. The HIS Admin can remove verification at any time; clinical login is blocked again.

## Security distinction

- MediFlow Doctor ID is an internal account identifier.
- NMC/IMR registration number is the external professional registration identifier.
- The PIN authenticates the MediFlow account; it does not prove professional registration by itself.
- The NMC/IMR check is deliberately not represented as an automated NMC API call. The current implementation records an administrator's confirmation after checking the official NMC registry.

## Production note

For a production hospital deployment, replace the manual confirmation step with an authorized NMC/NMR or State Medical Council verification integration if the hospital is granted access to one. Do not scrape or impersonate an NMC service and do not describe a manual check as live API verification.
