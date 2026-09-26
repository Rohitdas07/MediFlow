package com.mediflow.repository;

import com.mediflow.model.OpdDailySequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface OpdDailySequenceRepository extends JpaRepository<OpdDailySequence, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from OpdDailySequence s where s.sequenceDate = :date and s.careStream = :stream")
    Optional<OpdDailySequence> findForUpdate(@Param("date") LocalDate date, @Param("stream") String stream);
}
