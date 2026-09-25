package com.mediflow.dto;

import java.util.Map;

public class PatientCompatibilityResponse {
    private final boolean success;
    private final Map<String, Object> patient;
    private final Session session;
    private final String error;

    public PatientCompatibilityResponse(boolean success, Map<String,Object> patient, Session session, String error) {
        this.success = success; this.patient = patient; this.session = session; this.error = error;
    }
    public boolean isSuccess() { return success; }
    public Map<String,Object> getPatient() { return patient; }
    public Session getSession() { return session; }
    public String getError() { return error; }

    public static class Session {
        private final Long userId;
        private final Long patientId;
        private final String userName;
        private final String token;
        private final String loginTime;
        public Session(Long userId, Long patientId, String userName, String token, String loginTime) {
            this.userId=userId; this.patientId=patientId; this.userName=userName; this.token=token; this.loginTime=loginTime;
        }
        public Long getUserId(){return userId;}
        public Long getPatientId(){return patientId;}
        public String getUserName(){return userName;}
        public String getToken(){return token;}
        public String getLoginTime(){return loginTime;}
    }
}
