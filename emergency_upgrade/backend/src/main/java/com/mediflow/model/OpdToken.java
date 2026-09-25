package com.mediflow.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="opd_tokens", uniqueConstraints=@UniqueConstraint(name="uk_opd_day_token", columnNames={"token_date","token_number"}))
@Getter @Setter @NoArgsConstructor
public class OpdToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="token_date", nullable=false) private LocalDate tokenDate;
    @Column(name="token_number", nullable=false, length=30) private String tokenNumber;
    @Column(name="care_stream", nullable=false, length=30) private String careStream;
    @Column(name="patient_code", length=32) private String patientCode; // Permanent MFP-YYYY-XXXXXXXX; nullable only for unrecoverable legacy rows
    @Column(nullable=false) private LocalDateTime issuedAt=LocalDateTime.now();
}
