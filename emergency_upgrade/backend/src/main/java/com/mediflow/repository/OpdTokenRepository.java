package com.mediflow.repository;
import com.mediflow.model.OpdToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
public interface OpdTokenRepository extends JpaRepository<OpdToken,Long> {
    Optional<OpdToken> findByTokenDateAndPatientCode(LocalDate date, String patientCode);
    long countByTokenDateAndCareStream(LocalDate date, String careStream);
    boolean existsByTokenDateAndTokenNumber(LocalDate date, String tokenNumber);
}
