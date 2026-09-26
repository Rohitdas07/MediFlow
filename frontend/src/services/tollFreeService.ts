/**
 * MediFlow Toll-Free Emergency Gateway
 *
 * Provider-neutral contract:
 * REAL MODE:
 *   Telephony Provider -> POST /api/toll-free/calls/inbound -> MediFlow workflow
 *
 * The browser module uses the same call-session shape as the provider webhook,
 * so a provider such as Twilio/Exotel/Plivo can be connected later without
 * changing the emergency UI.
 */

import { mediflowFetch } from './mediflowApi';

export type TollFreeRisk = 'LOW' | 'MODERATE' | 'HIGH';

export type TollFreeStage =
  | 'incoming'
  | 'connecting'
  | 'assistant'
  | 'collection'
  | 'analysis'
  | 'risk'
  | 'hospital'
  | 'routing'
  | 'summary';

export interface TollFreePatientSummary {
  callId: string;
  tollFreeNumber: string;
  patientName: string;
  location: string;
  symptoms: string[];
  emergencyDetails: string;
  vitals: {
    bloodPressure: string;
    heartRate: string;
    spo2: string;
    temperature: string;
  };
  risk: TollFreeRisk;
  explanation: string;
  recommendedHospital: string;
  ambulanceStatus: string;
  ambulanceEta: string;
  assignedDoctor: string;
  createdAt: string;
}

export interface TollFreeCallSession {
  callId: string;
  number: string;
  startedAt: string;
  stage: TollFreeStage;
  assistantStatus: 'connecting' | 'connected' | 'collecting' | 'analyzing' | 'routing' | 'completed';
  summary: TollFreePatientSummary;
  transcript?: string[];
  savedAt?: string;
}

export const TOLL_FREE_NUMBER = '1800-123-MEDIFLOW';

export const createTollFreeCallSession = (): TollFreeCallSession => {
  const callId = `TF-${Date.now().toString(36).toUpperCase()}`;
  const summary: TollFreePatientSummary = {
    callId,
    tollFreeNumber: TOLL_FREE_NUMBER,
    patientName: 'Ananya Sharma',
    location: 'South Delhi, New Delhi',
    symptoms: ['Severe chest pressure', 'Breathlessness', 'Cold sweating'],
    emergencyDetails: 'Sudden onset chest pressure for approximately 20 minutes; patient is conscious and able to speak.',
    vitals: {
      bloodPressure: '158/98 mmHg',
      heartRate: '104 bpm',
      spo2: '93%',
      temperature: '98.6 °F'
    },
    risk: 'HIGH',
    explanation: 'Red-flag cardiac symptoms with elevated heart rate and reduced oxygen saturation require urgent emergency assessment.',
    recommendedHospital: 'AIIMS New Delhi — Emergency & Trauma Centre',
    ambulanceStatus: 'Ambulance dispatched',
    ambulanceEta: '8 minutes',
    assignedDoctor: 'Emergency Medicine Desk • Priority Bay 02',
    createdAt: new Date().toISOString()
  };

  return {
    callId,
    number: TOLL_FREE_NUMBER,
    startedAt: new Date().toISOString(),
    stage: 'incoming',
    assistantStatus: 'connecting',
    summary,
    transcript: [],
    savedAt: undefined
  };
};

export const saveTollFreeCallLocally = (session: TollFreeCallSession): void => {
  try {
    const saved = { ...session, savedAt: new Date().toISOString() };
    localStorage.setItem(`mediflow:tollfree:${session.callId}`, JSON.stringify(saved));
    localStorage.setItem('mediflow:tollfree:last', JSON.stringify(saved));
  } catch {
    // Storage can be unavailable in private/restricted browser contexts.
  }
};

export const loadLastTollFreeCall = (): TollFreeCallSession | null => {
  try {
    const raw = localStorage.getItem('mediflow:tollfree:last');
    return raw ? JSON.parse(raw) as TollFreeCallSession : null;
  } catch {
    return null;
  }
};

/**
 * Best-effort persistence hook. If a backend is configured, the call session
 * is posted to it. The browser flow remains online and usable when the backend
 * is temporarily unavailable, while the same payload remains provider-ready.
 */
export async function persistTollFreeCall(session: TollFreeCallSession): Promise<void> {
  try {
    await mediflowFetch('/api/toll-free/calls', {
      method: 'POST',
      body: JSON.stringify(session)
    });
  } catch {
    // UI simulation remains functional; real provider deployments should
    // configure VITE_MEDIFLOW_API_BASE_URL and the backend endpoint.
  }
}
