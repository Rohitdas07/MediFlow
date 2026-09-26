import { PatientProfile, PatientVitals, SymptomItem, RiskAssessment } from '../types';
import { checkRedFlagEmergency } from './emergencyTriage';

export type RiskLevel = 'LOW' | 'MODERATE' | 'HIGH';


const normalize = (value: string) => value.toLowerCase().trim();

const criticalSymptomPatterns = [
  /severe\s+chest\s+pain/,
  /crushing\s+chest\s+pain/,
  /cannot\s+breathe/,
  /can't\s+breathe/,
  /severe\s+(breathlessness|shortness of breath|difficulty breathing)/,
  /severe\s+bleeding/,
  /heavy\s+bleeding/,
  /uncontrolled\s+bleeding/,
  /stroke\b/,
  /facial\s+(droop|weakness)/,
  /slurred\s+speech/,
  /one[- ]sided\s+(weakness|paralysis|numbness)/,
  /altered\s+(mental\s+status|consciousness)/,
  /unconscious/,
  /unresponsive/,
  /fainted|fainting|loss of consciousness/,
  /seizure/,
  /coughing\s+blood/,
  /vomiting\s+blood/,
];

const concerningSymptomPatterns = [
  /chest\s+(pain|pressure|tightness|heaviness)/,
  /breathlessness|shortness of breath|difficulty breathing|breathing difficulty/,
  /dizziness|vertigo/,
  /persistent vomiting/,
  /high fever|fever/,
  /palpitations/,
  /moderate|severe pain/,
  /swelling with pain/,
];

function symptomText(symptoms: SymptomItem[]): string {
  return symptoms
    .flatMap(s => [s.name, s.character, s.associatedSymptoms?.join(' ') || ''])
    .filter(Boolean)
    .join(' ');
}

function assessSymptoms(symptoms: SymptomItem[]): { high: string[]; moderate: string[] } {
  const text = normalize(symptomText(symptoms));
  const high: string[] = [];
  const moderate: string[] = [];

  if (criticalSymptomPatterns.some(pattern => pattern.test(text)) || checkRedFlagEmergency(text, 'en').isEmergency) {
    high.push('Critical warning symptom reported');
  }

  symptoms.forEach(symptom => {
    if (symptom.severity >= 9) high.push(`Severe symptom: ${symptom.name}`);
    else if (symptom.severity >= 6) moderate.push(`Concerning symptom severity: ${symptom.name}`);
  });

  if (!high.length && concerningSymptomPatterns.some(pattern => pattern.test(text))) {
    moderate.push('Concerning symptom pattern reported');
  }

  return { high: [...new Set(high)], moderate: [...new Set(moderate)] };
}

function assessVitals(v: PatientVitals): { high: string[]; moderate: string[] } {
  const high: string[] = [];
  const moderate: string[] = [];

  if (v.spO2 !== undefined) {
    if (v.spO2 <= 90) high.push(`SpO₂ ${v.spO2}% is critically low`);
    else if (v.spO2 < 94) moderate.push(`SpO₂ ${v.spO2}% is below the usual target range`);
  }

  if (v.heartRate !== undefined) {
    if (v.heartRate < 40 || v.heartRate > 130) high.push(`Heart rate ${v.heartRate} bpm is critically abnormal`);
    else if (v.heartRate < 50 || v.heartRate > 110) moderate.push(`Heart rate ${v.heartRate} bpm is outside the usual resting range`);
  }

  if (v.respiratoryRate !== undefined) {
    if (v.respiratoryRate >= 30 || v.respiratoryRate < 8) high.push(`Respiratory rate ${v.respiratoryRate}/min is critically abnormal`);
    else if (v.respiratoryRate >= 22 || v.respiratoryRate < 12) moderate.push(`Respiratory rate ${v.respiratoryRate}/min is abnormal`);
  }

  if (v.temperature !== undefined) {
    if (v.temperature >= 104 || v.temperature < 95) high.push(`Temperature ${v.temperature}°F is critically abnormal`);
    else if (v.temperature >= 101 || v.temperature < 96) moderate.push(`Temperature ${v.temperature}°F is abnormal`);
  }

  const sys = v.bpSystolic;
  const dia = v.bpDiastolic;
  if (sys !== undefined || dia !== undefined) {
    if ((sys !== undefined && (sys >= 180 || sys <= 80)) || (dia !== undefined && (dia >= 120 || dia <= 50))) {
      high.push(`Blood pressure ${sys ?? '—'}/${dia ?? '—'} mmHg is critically abnormal`);
    } else if ((sys !== undefined && (sys >= 160 || sys < 90)) || (dia !== undefined && (dia >= 100 || dia < 60))) {
      moderate.push(`Blood pressure ${sys ?? '—'}/${dia ?? '—'} mmHg is abnormal`);
    }
  }

  return { high: [...new Set(high)], moderate: [...new Set(moderate)] };
}

export function assessPatientRisk(patient: Pick<PatientProfile, 'age' | 'symptoms' | 'vitals' | 'pastIllnesses'>): RiskAssessment {
  const symptoms = patient.symptoms || [];
  const vitals = patient.vitals || {};
  const history = patient.pastIllnesses || [];
  const symptomResult = assessSymptoms(symptoms);
  const vitalResult = assessVitals(vitals);
  const highFactors = [...symptomResult.high, ...vitalResult.high];
  const moderateFactors = [...symptomResult.moderate, ...vitalResult.moderate];

  // Missing inputs do not automatically increase risk; they only make the assessment incomplete.
  const incompleteData = [
    symptoms.length === 0,
    vitals.heartRate === undefined,
    vitals.spO2 === undefined,
    vitals.temperature === undefined,
    vitals.bpSystolic === undefined || vitals.bpDiastolic === undefined,
    vitals.respiratoryRate === undefined,
    patient.age === undefined || patient.age === null,
  ].some(Boolean);

  // History can lower the threshold for concern when combined with abnormal findings.
  const historyText = normalize(history.join(' '));
  const significantHistory = /(heart|cardiac|stroke|asthma|copd|lung|diabetes|kidney|renal|pregnan|cancer)/.test(historyText);
  const ageConcern = patient.age < 5 || patient.age >= 75;
  if (ageConcern && moderateFactors.length >= 1) {
    moderateFactors.push(`Age ${patient.age} may increase concern when combined with abnormal findings`);
  }
  if (significantHistory && moderateFactors.length >= 1) {
    moderateFactors.push('Relevant medical history may increase clinical concern');
  }

  const availableClinicalData = symptoms.length > 0 || Object.values(vitals).some(value => value !== undefined);
  let level: RiskLevel = 'LOW';
  if (highFactors.length > 0) {
    level = 'HIGH';
  } else if (moderateFactors.length >= 2 || (moderateFactors.length === 1 && significantHistory)) {
    level = 'MODERATE';
  } else if (moderateFactors.length === 1) {
    level = 'MODERATE';
  } else if (!availableClinicalData) {
    level = 'MODERATE';
  }

  const factors = [...new Set([...highFactors, ...moderateFactors])];
  let reason: string;
  let recommendedAction: string;

  if (level === 'HIGH') {
    reason = factors.slice(0, 2).join('; ') || 'A potential emergency warning sign was identified.';
    recommendedAction = 'Seek immediate medical attention. Alert the emergency/triage team and consider emergency services when appropriate.';
  } else if (level === 'MODERATE') {
    reason = factors.slice(0, 2).join('; ') || 'Symptoms or vital signs are concerning but do not clearly indicate an emergency.';
    recommendedAction = 'Arrange medical evaluation soon and continue clinical monitoring while awaiting assessment.';
  } else {
    reason = 'No major warning signs were identified from the information currently available.';
    recommendedAction = 'Routine consultation is appropriate, with reassessment if symptoms worsen or new warning signs appear.';
  }

  if (incompleteData) {
    reason += ' Assessment is based on incomplete data.';
  }

  return {
    level,
    reason,
    recommendedAction,
    incompleteData,
    evaluatedAt: new Date().toISOString(),
    factors,
  };
}
