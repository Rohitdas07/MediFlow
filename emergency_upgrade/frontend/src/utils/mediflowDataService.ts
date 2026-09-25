/**
 * MediFlow data adapter.
 *
 * This file intentionally contains NO MediFlowBackend client and NO separate Node/Express
 * backend. All server persistence is handled by the MediFlowBackend + MySQL
 * backend. The historical export names are retained temporarily so existing
 * MediFlow UI modules continue to compile while they are migrated to the newer
 * service names.
 */
import {
  Appointment,
  PrescriptionRecord,
  PatientProfile,
  PatientOneYearSummary,
  HospitalStaffMember,
  HospitalSystemConfig,
} from '../types';
import { mediflowFetch } from '../services/mediflowApi';
import { getStoredAuthSession } from './authStorage';

export const MEDIFLOW_BACKEND_NAME = 'MediFlow Spring Boot + MySQL';
export const MEDIFLOW_API_URL = (import.meta.env.VITE_MEDIFLOW_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
export const LEGACY_BACKEND_KEY = '';
export const MEDIFLOW_SQL_NOTE = '-- MediFlow uses MySQL through Spring Boot. No separate backend schema is required.';
export const MEDIFLOW_STAFF_SQL_NOTE = '-- Staff/doctor persistence is handled by the MediFlowBackend backend.';

function authHeaders(): Record<string,string> {
  const token = getStoredAuthSession()?.token;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// Compatibility adapter for legacy modules; it never opens a second backend connection.
export const legacyDataAdapter = {
  from(_table: string) {
    const result = { data: null as any, error: { message: 'Legacy MediFlowBackend adapter disabled; use MediFlowBackend.' } };
    const chain: any = {
      select: () => chain,
      eq: () => chain,
      order: () => chain,
      limit: () => Promise.resolve(result),
      upsert: () => chain,
      delete: () => chain,
      then: (resolve: any) => Promise.resolve(result).then(resolve),
    };
    return chain;
  }
};

export interface AmbulanceBookingRecord {
  id: string;
  booking_id: string;
  patient_name: string;
  patient_phone: string;
  pickup_address: string;
  pickup_lat?: number;
  pickup_lng?: number;
  pickupLocation?: { latitude: number; longitude: number };
  destination_hospital: string;
  destinationHospital?: any;
  ambulance_tier: 'basic' | 'advanced' | 'neonatal';
  fare_inr: number;
  eta_minutes: number;
  is_critical: boolean;
  condition_notes?: string;
  driver_name: string;
  driver_phone: string;
  vehicle_number: string;
  status: 'booked' | 'driver_assigned' | 'en_route' | 'arrived' | 'completed';
  created_at: string;
}

export interface TriageRecordPayload {
  id?: string;
  patient_id: string;
  patient_name: string;
  age?: number;
  gender?: string;
  token_number?: string;
  triage_level: 'CRITICAL_EMERGENCY' | 'URGENT_PRIORITY' | 'STANDARD_OPD';
  priority_score: number;
  vitals?: any;
  red_flags?: string[];
  chief_complaint?: string;
  destination_department?: string;
  status?: string;
  assigned_nurse?: string;
  clinical_rationale?: string;
  immediate_actions?: string[];
  assessed_at?: string;
}

export async function testMediFlowBackendConnection() {
  try {
    await mediflowFetch('/api/departments');
    return { connected: true, tableExists: true, tableDetails: {}, message: 'Connected to MediFlowBackend + MySQL.' };
  } catch (e:any) {
    return { connected: false, tableExists: false, message: e?.message || 'MediFlow backend is unavailable.' };
  }
}

export async function saveAppointmentToMediFlowBackend(appointment: Appointment) {
  // Appointment booking is performed by /api/appointments in the Spring backend.
  return { success: true, data: appointment, isDbPersisted: false };
}

export async function fetchAppointmentsFromMediFlowBackend(): Promise<{appointments: Appointment[]; error?: string}> {
  try {
    const data = await mediflowFetch<any[]>('/api/appointments', { headers: authHeaders() });
    return { appointments: Array.isArray(data) ? data : [] };
  } catch (e:any) {
    return { appointments: [], error: e?.message || 'Failed to load appointments.' };
  }
}

export async function saveAmbulanceBookingToMediFlowBackend(booking: AmbulanceBookingRecord) {
  return { success: true, data: booking, isDbPersisted: false };
}
export async function fetchAmbulanceBookingsFromMediFlowBackend(): Promise<{bookings: AmbulanceBookingRecord[]; error?: string}> {
  try {
    const data = await mediflowFetch<any[]>('/api/ambulances/bookings', { headers: authHeaders() });
    return { bookings: Array.isArray(data) ? data as AmbulanceBookingRecord[] : [] };
  } catch (e:any) { return { bookings: [], error: e?.message || 'Failed to load ambulance bookings.' }; }
}
export async function updateAmbulanceStatusInMediFlowBackend(_bookingId: string, _status: string) {
  return { success: false, error: 'Ambulance status updates are managed by the MediFlowBackend service.' };
}

export async function savePrescriptionToMediFlowBackend(prescription: PrescriptionRecord) {
  return { success: true, data: prescription, isDbPersisted: false };
}
export async function fetchPrescriptionsFromMediFlowBackend(_patientId?: string): Promise<{prescriptions: PrescriptionRecord[]; error?: string}> {
  return { prescriptions: [] };
}

export async function savePatientProfileToMediFlowBackend(patient: PatientProfile) {
  return { success: true, data: patient, isDbPersisted: false };
}
export async function lookupPatientInMediFlowBackend(_query: string) { return { error: 'Use MediFlow patient authentication.' }; }
export async function fetchPatientProfileFromMediFlowBackend(_patientId: string) { return { error: 'Use MediFlow patient authentication.' }; }
export async function saveOneYearSummaryToMediFlowBackend(summary: PatientOneYearSummary) {
  return { success: true, data: summary, isDbPersisted: false };
}
export async function saveTriageAssessmentToMediFlowBackend(triage: TriageRecordPayload) {
  return { success: true, data: triage, isDbPersisted: false };
}

export async function saveStaffMemberToMediFlowBackend(staff: HospitalStaffMember) {
  return { success: true, data: staff, isDbPersisted: false };
}
export async function fetchStaffMembersFromMediFlowBackend(): Promise<{staff: HospitalStaffMember[]; error?: string}> {
  return { staff: [], error: 'Staff persistence endpoint is not yet exposed by the current Spring Boot backend.' };
}
export async function fetchDoctorsFromMediFlowBackendView() { return { doctors: [] as any[] }; }
export async function syncAllStaffToMediFlowBackend(_staff: HospitalStaffMember[]) { return { success: false, error: 'Not applicable: MediFlowBackend is disabled.' }; }
export async function deleteStaffMemberFromMediFlowBackend(_staffId: string) { return { success: false, error: 'Staff delete endpoint is not yet exposed by the current Spring Boot backend.' }; }
export async function saveHospitalConfigToMediFlowBackend(config: HospitalSystemConfig) { return { success: true, data: config, isDbPersisted: false }; }
export async function fetchHospitalConfigFromMediFlowBackend(): Promise<{config: HospitalSystemConfig|null; error?: string}> { return { config: null }; }

