package com.mediflow.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "opd_daily_sequences", uniqueConstraints = @UniqueConstraint(
        name = "uk_opd_sequence_day_stream", columnNames = {"sequence_date", "care_stream"}
))
@Getter
@Setter
@NoArgsConstructor
public class OpdDailySequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_date", nullable = false)
    private LocalDate sequenceDate;

    @Column(name = "care_stream", nullable = false, length = 30)
    private String careStream;

    @Column(name = "last_number", nullable = false)
    private Long lastNumber = 0L;
}
