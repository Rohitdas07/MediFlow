import {
  testMediFlowBackendConnection as testBackendConnection,
  fetchAppointmentsFromMediFlowBackend as fetchAppointments,
  fetchPrescriptionsFromMediFlowBackend as fetchPrescriptions,
  saveAppointmentToMediFlowBackend as saveAppointment,
  savePrescriptionToMediFlowBackend as savePrescription,
  MEDIFLOW_BACKEND_NAME,
  MEDIFLOW_API_URL,
  MEDIFLOW_SQL_NOTE,
} from './mediflowDataService';

export { MEDIFLOW_BACKEND_NAME, MEDIFLOW_API_URL, MEDIFLOW_SQL_NOTE };

export const testMediFlowBackendConnection = testBackendConnection;
export const fetchAppointmentsFromMediFlowBackend = fetchAppointments;
export const fetchPrescriptionsFromMediFlowBackend = fetchPrescriptions;
export const saveAppointmentToMediFlowBackend = saveAppointment;
export const savePrescriptionToMediFlowBackend = savePrescription;
