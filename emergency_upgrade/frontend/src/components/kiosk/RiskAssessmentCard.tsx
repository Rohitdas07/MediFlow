import React from 'react';
import { AlertTriangle, CheckCircle2, Info, ShieldAlert } from 'lucide-react';
import { PatientProfile } from '../../types';
import { assessPatientRisk } from '../../utils/riskAssessment';

interface RiskAssessmentCardProps {
  patient: PatientProfile;
  onAssessment?: (assessment: ReturnType<typeof assessPatientRisk>) => void;
}

export const RiskAssessmentCard: React.FC<RiskAssessmentCardProps> = ({ patient, onAssessment }) => {
  const assessment = patient.riskAssessment || assessPatientRisk(patient);
  React.useEffect(() => {
    if (!patient.riskAssessment) onAssessment?.(assessment);
  }, [patient.riskAssessment, assessment, onAssessment]);

  const styles = {
    LOW: {
      wrap: 'border-emerald-200 bg-emerald-50',
      badge: 'bg-emerald-600 text-white',
      icon: <CheckCircle2 className="w-5 h-5 text-emerald-700" />,
    },
    MODERATE: {
      wrap: 'border-amber-200 bg-amber-50',
      badge: 'bg-amber-500 text-white',
      icon: <Info className="w-5 h-5 text-amber-700" />,
    },
    HIGH: {
      wrap: 'border-rose-200 bg-rose-50',
      badge: 'bg-rose-600 text-white',
      icon: <ShieldAlert className="w-5 h-5 text-rose-700" />,
    },
  }[assessment.level];

  return (
    <section className={`rounded-2xl border-2 p-5 ${styles.wrap}`} aria-label="MediFlow risk assessment">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <div className="mt-0.5">{styles.icon}</div>
          <div>
            <p className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500">Risk Assessment</p>
            <h3 className="text-lg font-extrabold text-slate-900">Triage / Risk Assessment</h3>
          </div>
        </div>
        <span className={`rounded-full px-4 py-1.5 text-sm font-extrabold tracking-wide ${styles.badge}`}>
          {assessment.level}
        </span>
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <div className="rounded-xl bg-white/80 p-3 border border-white">
          <p className="text-[10px] font-extrabold uppercase tracking-wider text-slate-500">Reason</p>
          <p className="mt-1 text-sm font-semibold text-slate-800 leading-relaxed">{assessment.reason}</p>
        </div>
        <div className="rounded-xl bg-white/80 p-3 border border-white">
          <p className="text-[10px] font-extrabold uppercase tracking-wider text-slate-500">Recommended Action</p>
          <p className="mt-1 text-sm font-semibold text-slate-800 leading-relaxed">{assessment.recommendedAction}</p>
        </div>
      </div>

      {assessment.factors.length > 0 && (
        <div className="mt-3 rounded-xl bg-white/70 p-3">
          <p className="text-[10px] font-extrabold uppercase tracking-wider text-slate-500 mb-1">Assessment factors</p>
          <ul className="list-disc list-inside text-xs text-slate-700 space-y-0.5">
            {assessment.factors.slice(0, 4).map((factor, index) => <li key={`${factor}-${index}`}>{factor}</li>)}
          </ul>
        </div>
      )}

      <div className="mt-3 flex items-start gap-2 rounded-xl bg-white/80 border border-slate-200 p-3 text-[11px] text-slate-600">
        <AlertTriangle className="w-4 h-4 shrink-0 text-slate-500 mt-0.5" />
        <p><strong>Not a medical diagnosis.</strong> This is a triage/risk assessment to help prioritize care. Clinical staff should verify the information and make the final clinical decision.</p>
      </div>
    </section>
  );
};
