-- MediFlow core MySQL persistence setup.
-- Run once if you want to prepare MySQL before starting Spring Boot.
-- Hibernate ddl-auto=update will also create/update these tables automatically.

CREATE DATABASE IF NOT EXISTS mediflow_ai
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE mediflow_ai;

CREATE TABLE IF NOT EXISTS patients (
  id BIGINT NOT NULL AUTO_INCREMENT,
  patient_code VARCHAR(32) NULL,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(255) NULL,
  dob DATE NULL,
  gender VARCHAR(255) NULL,
  created_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_patients_patient_code (patient_code),
  UNIQUE KEY uk_patients_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hospital_staff (
  id BIGINT NOT NULL AUTO_INCREMENT,
  staff_id VARCHAR(40) NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  role VARCHAR(30) NOT NULL,
  role_title VARCHAR(255) NULL,
  department VARCHAR(255) NULL,
  specialization VARCHAR(255) NULL,
  registration_number VARCHAR(255) NULL,
  employee_code VARCHAR(255) NULL,
  mobile VARCHAR(255) NULL,
  email VARCHAR(255) NULL,
  qualification VARCHAR(255) NULL,
  joining_date DATE NULL,
  room_number VARCHAR(255) NULL,
  opd_timings VARCHAR(255) NULL,
  consultation_fee DECIMAL(19,2) NULL,
  available_days VARCHAR(1000) NULL,
  bio VARCHAR(4000) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  status_reason VARCHAR(255) NULL,
  last_login_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  pin_hash VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_staff_staff_id (staff_id),
  UNIQUE KEY uk_staff_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS opd_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  token_date DATE NOT NULL,
  token_number VARCHAR(30) NOT NULL,
  care_stream VARCHAR(30) NOT NULL,
  patient_code VARCHAR(32) NULL,
  issued_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_opd_day_token (token_date, token_number),
  KEY idx_opd_patient_day (token_date, patient_code),
  CONSTRAINT fk_opd_patient_code FOREIGN KEY (patient_code) REFERENCES patients(patient_code)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS opd_daily_sequences (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sequence_date DATE NOT NULL,
  care_stream VARCHAR(30) NOT NULL,
  last_number BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_opd_sequence_day_stream (sequence_date, care_stream)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_staff_role_status ON hospital_staff(role, status);
CREATE INDEX idx_staff_department ON hospital_staff(department);
CREATE INDEX idx_patient_phone ON patients(phone);
