import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Activity, Ambulance, BrainCircuit, Bot, CheckCircle2, Clock3, Hospital, MapPin,
  Mic, Phone, PhoneCall, PhoneOff, Save, ShieldAlert, Square, UserRound, Volume2, X
} from 'lucide-react';
import {
  createTollFreeCallSession, persistTollFreeCall, saveTollFreeCallLocally,
  TollFreeCallSession, TollFreeStage
} from '../../services/tollFreeService';
import { hasSpeechRecognition, listenOnce, speakEmergency, stopEmergencyVoice } from '../../utils/emergencyVoice';

interface TollFreeEmergencyModalProps { isOpen: boolean; onClose: () => void; }

const stages: Array<{ key: TollFreeStage; label: string }> = [
  { key: 'incoming', label: 'Incoming Call' }, { key: 'connecting', label: 'Connecting' },
  { key: 'assistant', label: 'AI Assistant Connected' }, { key: 'collection', label: 'Information Collection' },
  { key: 'analysis', label: 'AI Analysis' }, { key: 'risk', label: 'Risk Classification' },
  { key: 'hospital', label: 'Hospital Recommendation' }, { key: 'routing', label: 'Ambulance / Doctor Routing' },
  { key: 'summary', label: 'Digital Patient Summary' }
];
const stageIndex = (stage: TollFreeStage) => stages.findIndex(s => s.key === stage);

const assistantText = (session: TollFreeCallSession) => {
  const s = session.summary;
  switch (session.stage) {
    case 'incoming': return 'Incoming emergency call detected. Please stay on the line.';
    case 'connecting': return 'Connecting you to the MediFlow emergency voice assistant.';
    case 'assistant': return 'Hello. I am the MediFlow emergency assistant. I will collect a few important details.';
    case 'collection': return s.patientName ? `Thank you, ${s.patientName}. Please confirm the remaining emergency details.` : 'Please enter the patient name, location, symptoms, emergency details and available vital signs.';
    case 'analysis': return 'Thank you. I am analyzing the information now and preparing the emergency summary.';
    case 'risk': return `The collected information has been classified as ${s.risk} risk. ${s.explanation}`;
    case 'hospital': return `The recommended emergency destination is ${s.recommendedHospital}.`;
    case 'routing': return `${s.ambulanceStatus}. Current estimated arrival is ${s.ambulanceEta}.`;
    default: return 'The digital patient summary is ready and has been saved for authorized clinical users.';
  }
};

const classifyEmergency = (summary: TollFreeCallSession['summary']): { risk: TollFreeCallSession['summary']['risk']; explanation: string } => {
  const text = `${summary.symptoms.join(' ')} ${summary.emergencyDetails}`.toLowerCase();
  const highTerms = ['chest pain', 'chest pressure', 'breathlessness', 'difficulty breathing', 'unconscious', 'stroke', 'seizure', 'severe bleeding', 'collapse'];
  const moderateTerms = ['fever', 'vomiting', 'moderate pain', 'dizziness', 'dehydration'];
  const high = highTerms.some(term => text.includes(term)) || Number.parseInt(summary.vitals.spo2) > 0 && Number.parseInt(summary.vitals.spo2) < 94 || Number.parseInt(summary.vitals.heartRate) >= 120;
  if (high) return { risk: 'HIGH', explanation: 'Red-flag symptoms or concerning available vital signs require urgent emergency assessment.' };
  if (moderateTerms.some(term => text.includes(term))) return { risk: 'MODERATE', explanation: 'The information contains symptoms that warrant timely clinical assessment.' };
  return { risk: 'LOW', explanation: 'No high-risk signal was detected from the information currently provided; clinical review is still recommended.' };
};

export const TollFreeEmergencyModal: React.FC<TollFreeEmergencyModalProps> = ({ isOpen, onClose }) => {
  const [session, setSession] = useState<TollFreeCallSession | null>(null);
  const [seconds, setSeconds] = useState(0);
  const [voiceEnabled, setVoiceEnabled] = useState(true);
  const [isListening, setIsListening] = useState(false);
  const [saved, setSaved] = useState(false);
  const lastSpokenStage = useRef<TollFreeStage | null>(null);

  const startCall = () => {
    const seeded = createTollFreeCallSession();
    const next: TollFreeCallSession = {
      ...seeded,
      summary: {
        ...seeded.summary,
        patientName: '',
        location: '',
        symptoms: [],
        emergencyDetails: '',
        vitals: { bloodPressure: '', heartRate: '', spo2: '', temperature: '' }
      }
    };
    setSession(next); setSeconds(0); setSaved(false);
    saveTollFreeCallLocally(next); void persistTollFreeCall(next);
  };

  useEffect(() => {
    if (!isOpen) { stopEmergencyVoice(); setSession(null); setSeconds(0); return; }
    startCall();
    return () => stopEmergencyVoice();
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen || !session) return;
    const timer = window.setInterval(() => setSeconds(s => s + 1), 1000);
    return () => window.clearInterval(timer);
  }, [isOpen, session]);

  // The automated flow pauses at Information Collection so the presenter can fill the form.
  useEffect(() => {
    if (!isOpen || !session) return;
    const current = stageIndex(session.stage);
    if (session.stage === 'collection' || current >= stages.length - 1) return;
    const delay = session.stage === 'incoming' ? 1200 : 1800;
    const timer = window.setTimeout(() => advanceStage(), delay);
    return () => window.clearTimeout(timer);
  }, [session, isOpen]);

  useEffect(() => {
    if (!session || !voiceEnabled || lastSpokenStage.current === session.stage) return;
    lastSpokenStage.current = session.stage;
    const text = assistantText(session);
    const timer = window.setTimeout(() => speakEmergency(text), 120);
    return () => window.clearTimeout(timer);
  }, [session?.stage, voiceEnabled]);

  const updateSession = (next: TollFreeCallSession) => {
    setSession(next); saveTollFreeCallLocally(next); void persistTollFreeCall(next); setSaved(true);
  };

  const advanceStage = () => {
    if (!session) return;
    const current = stageIndex(session.stage);
    if (current >= stages.length - 1) return;
    const nextStage = stages[current + 1].key;
    const classified = nextStage === 'risk' ? classifyEmergency(session.summary) : null;
    const nextSummary = classified ? { ...session.summary, risk: classified.risk, explanation: classified.explanation } : session.summary;
    const next: TollFreeCallSession = {
      ...session, stage: nextStage, summary: nextSummary,
      assistantStatus: nextStage === 'incoming' || nextStage === 'connecting' ? 'connecting' :
        nextStage === 'assistant' ? 'connected' : nextStage === 'collection' ? 'collecting' :
        nextStage === 'analysis' || nextStage === 'risk' ? 'analyzing' :
        nextStage === 'hospital' || nextStage === 'routing' ? 'routing' : 'completed'
    };
    updateSession(next);
  };

  const updateField = (field: keyof TollFreeCallSession['summary'], value: string | string[]) => {
    if (!session) return;
    const summary = { ...session.summary } as any;
    summary[field] = value;
    updateSession({ ...session, summary, transcript: [...(session.transcript || []), `${field}: ${value}`] });
  };

  const updateVital = (field: keyof TollFreeCallSession['summary']['vitals'], value: string) => {
    if (!session) return;
    const summary = { ...session.summary, vitals: { ...session.summary.vitals, [field]: value } };
    updateSession({ ...session, summary });
  };

  const fillByVoice = (field: keyof TollFreeCallSession['summary']) => {
    if (!session || !hasSpeechRecognition()) return;
    setIsListening(true);
    speakEmergency(`Please say the ${String(field).replace(/([A-Z])/g, ' $1').toLowerCase()}.`);
    listenOnce(text => { setIsListening(false); updateField(field, field === 'symptoms' ? text.split(',').map(x => x.trim()).filter(Boolean) : text); speakEmergency(`I heard ${text}. I have updated the ${String(field).replace(/([A-Z])/g, ' $1').toLowerCase()}.`); });
  };

  const progress = useMemo(() => session ? ((stageIndex(session.stage) + 1) / stages.length) * 100 : 0, [session]);
  if (!isOpen || !session) return null;

  const summary = session.summary;
  const endCall = () => { saveTollFreeCallLocally(session); void persistTollFreeCall({ ...session, stage: 'summary', savedAt: new Date().toISOString() }); stopEmergencyVoice(); onClose(); };
  const speakCurrent = () => { lastSpokenStage.current = session.stage; speakEmergency(assistantText(session)); };
  const canContinue = Boolean(summary.patientName.trim() && summary.location.trim() && summary.symptoms.length && summary.emergencyDetails.trim());

  return (
    <div className="fixed inset-0 z-[100] bg-slate-950/60 backdrop-blur-sm flex items-center justify-center p-3 sm:p-6">
      <div className="w-full max-w-6xl max-h-[94vh] overflow-hidden rounded-3xl bg-white shadow-2xl border border-slate-200">
        <div className="bg-gradient-to-r from-teal-800 to-teal-700 text-white px-5 sm:px-7 py-5">
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 text-teal-100 text-xs font-bold uppercase tracking-wider"><PhoneCall className="w-4 h-4" /> Toll-Free Emergency Service</div>
              <h2 className="mt-1 text-xl sm:text-2xl font-extrabold">MediFlow Emergency Call</h2>
              <p className="text-teal-100 text-xs sm:text-sm mt-1">Provider-ready voice workflow • {session.number}</p>
            </div>
            <button onClick={endCall} className="p-2 rounded-xl hover:bg-white/10" aria-label="Close call"><X className="w-5 h-5" /></button>
          </div>
          <div className="mt-5 h-1.5 rounded-full bg-white/20 overflow-hidden"><div className="h-full bg-white rounded-full transition-all duration-700" style={{ width: `${progress}%` }} /></div>
          <div className="mt-2 flex items-center justify-between text-[11px] text-teal-100"><span>{stages[stageIndex(session.stage)].label}</span><span className="font-mono">{String(Math.floor(seconds / 60)).padStart(2, '0')}:{String(seconds % 60).padStart(2, '0')}</span></div>
        </div>

        <div className="p-4 sm:p-6 overflow-y-auto max-h-[calc(94vh-132px)] bg-slate-50">
          <div className="grid grid-cols-1 lg:grid-cols-[1fr_1.45fr] gap-4">
            <div className="space-y-4">
              <div className="rounded-2xl bg-white border border-slate-200 p-5 shadow-sm">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-3"><div className="relative w-12 h-12 rounded-full bg-teal-50 text-teal-700 flex items-center justify-center"><Phone className="w-5 h-5" />{session.stage !== 'summary' && <span className="absolute inset-0 rounded-full border-2 border-teal-300 animate-ping" />}</div><div><p className="font-extrabold text-slate-900">AI Voice Assistant</p><p className="text-xs text-slate-500">{session.assistantStatus === 'connected' ? 'Connected & listening' : session.assistantStatus === 'collecting' ? 'Collecting emergency information' : session.assistantStatus === 'analyzing' ? 'Analyzing clinical signals' : session.assistantStatus === 'routing' ? 'Coordinating care' : session.assistantStatus === 'completed' ? 'Call workflow completed' : 'Connecting'}</p></div></div>
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 text-[11px] font-bold"><span className="w-1.5 h-1.5 rounded-full bg-emerald-500" /> LIVE</span>
                </div>
                <div className="mt-4 flex flex-wrap gap-2">
                  <button onClick={() => { setVoiceEnabled(v => !v); if (voiceEnabled) stopEmergencyVoice(); }} className="inline-flex items-center gap-2 px-3 py-2 rounded-xl border border-slate-200 text-xs font-bold text-slate-700 hover:bg-slate-50"><Volume2 className="w-4 h-4 text-teal-700" /> {voiceEnabled ? 'Voice On' : 'Voice Off'}</button>
                  <button onClick={speakCurrent} className="inline-flex items-center gap-2 px-3 py-2 rounded-xl bg-teal-50 text-teal-800 text-xs font-bold hover:bg-teal-100"><Volume2 className="w-4 h-4" /> Speak</button>
                  {isListening && <span className="inline-flex items-center gap-2 px-3 py-2 rounded-xl bg-rose-50 text-rose-700 text-xs font-bold"><Mic className="w-4 h-4 animate-pulse" /> Listening…</span>}
                </div>
                <div className="mt-4 rounded-xl bg-slate-900 text-slate-100 p-4"><div className="flex items-center gap-2 text-teal-300 text-xs font-bold"><Bot className="w-4 h-4" /> Voice assistant</div><p className="mt-2 text-sm leading-relaxed">{assistantText(session)}</p></div>
              </div>

              <div className="rounded-2xl bg-white border border-slate-200 p-5 shadow-sm">
                <div className="flex items-center justify-between"><div className="flex items-center gap-2 font-extrabold text-slate-900"><Activity className="w-4 h-4 text-teal-700" /> Call information</div><span className="text-[10px] font-bold text-emerald-700">{saved ? 'Saved' : 'Saving…'}</span></div>
                <div className="mt-3 grid grid-cols-2 gap-3 text-xs"><Info label="Patient" value={summary.patientName} icon={<UserRound className="w-3.5 h-3.5" />} /><Info label="Location" value={summary.location} icon={<MapPin className="w-3.5 h-3.5" />} /><Info label="Toll-Free" value={summary.tollFreeNumber} icon={<Phone className="w-3.5 h-3.5" />} /><Info label="Call ID" value={summary.callId} icon={<ShieldAlert className="w-3.5 h-3.5" />} /></div>
              </div>

              <div className="rounded-2xl bg-white border border-slate-200 p-4"><p className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-3">Emergency workflow</p><div className="space-y-2">{stages.map((item, index) => { const active = index === stageIndex(session.stage); const done = index < stageIndex(session.stage); return <div key={item.key} className={`flex items-center gap-2.5 text-xs ${active ? 'text-teal-800 font-bold' : done ? 'text-emerald-700' : 'text-slate-400'}`}>{done ? <CheckCircle2 className="w-4 h-4" /> : active ? <span className="w-4 h-4 rounded-full bg-teal-600 ring-4 ring-teal-50" /> : <span className="w-4 h-4 rounded-full border border-slate-300" />}<span>{item.label}</span></div>; })}</div></div>
            </div>

            <div className="space-y-4">
              {session.stage === 'collection' && (
                <div className="rounded-2xl bg-white border-2 border-teal-100 p-5 shadow-sm">
                  <div className="flex items-start justify-between gap-3"><div><h3 className="font-extrabold text-slate-900">Information Collection</h3><p className="text-xs text-slate-500 mt-1">Fill the details below. The voice assistant will immediately use the updated information and speak the next prompt.</p></div><span className="px-2.5 py-1 rounded-full bg-teal-50 text-teal-800 text-[10px] font-bold">VOICE + FORM</span></div>
                  <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <VoiceField label="Patient name" value={summary.patientName} onChange={v => updateField('patientName', v)} onVoice={() => fillByVoice('patientName')} onCommit={() => speakEmergency(`Thank you, ${summary.patientName || 'patient'}. Please enter the location.`)} />
                    <VoiceField label="Location" value={summary.location} onChange={v => updateField('location', v)} onVoice={() => fillByVoice('location')} onCommit={() => speakEmergency('Thank you. Please enter the symptoms.')}/>
                    <VoiceField label="Symptoms" value={summary.symptoms.join(', ')} onChange={v => updateField('symptoms', v.split(',').map(x => x.trim()).filter(Boolean))} onVoice={() => fillByVoice('symptoms')} onCommit={() => speakEmergency('Thank you. Please describe the emergency details.')}/>
                    <VoiceField label="Emergency details" value={summary.emergencyDetails} onChange={v => updateField('emergencyDetails', v)} onVoice={() => fillByVoice('emergencyDetails')} onCommit={() => speakEmergency('Thank you. Please confirm the available vital signs.') } full />
                    <VitalField label="Blood pressure" value={summary.vitals.bloodPressure} onChange={v => updateVital('bloodPressure', v)} />
                    <VitalField label="Heart rate" value={summary.vitals.heartRate} onChange={v => updateVital('heartRate', v)} />
                    <VitalField label="SpO₂" value={summary.vitals.spo2} onChange={v => updateVital('spo2', v)} />
                    <VitalField label="Temperature" value={summary.vitals.temperature} onChange={v => updateVital('temperature', v)} />
                  </div>
                  <div className="mt-4 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3"><p className="text-[11px] text-slate-500">{hasSpeechRecognition() ? 'Mic input is available in this browser.' : 'Mic input is unavailable in this browser; you can still use the form and voice output.'}</p><button disabled={!canContinue} onClick={advanceStage} className="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-teal-700 hover:bg-teal-800 disabled:bg-slate-300 text-white text-sm font-extrabold"><CheckCircle2 className="w-4 h-4" /> Continue to AI Analysis</button></div>
                </div>
              )}

              <div className="rounded-2xl bg-white border border-slate-200 p-5 shadow-sm"><div className="flex items-center justify-between gap-3"><h3 className="font-extrabold text-slate-900 flex items-center gap-2"><BrainCircuit className="w-5 h-5 text-teal-700" /> Clinical information</h3><span className={`px-3 py-1 rounded-full text-xs font-extrabold ${summary.risk === 'HIGH' ? 'bg-rose-100 text-rose-700' : summary.risk === 'MODERATE' ? 'bg-amber-100 text-amber-700' : 'bg-emerald-100 text-emerald-700'}`}>{summary.risk} RISK</span></div><div className="mt-4"><p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Symptoms</p><div className="mt-2 flex flex-wrap gap-2">{summary.symptoms.map(s => <span key={s} className="px-2.5 py-1 rounded-lg bg-rose-50 text-rose-700 text-xs font-semibold">{s}</span>)}</div></div><div className="mt-4"><p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Available vital signs</p><div className="mt-2 grid grid-cols-2 sm:grid-cols-4 gap-2"><Vital label="BP" value={summary.vitals.bloodPressure} /><Vital label="Heart rate" value={summary.vitals.heartRate} /><Vital label="SpO₂" value={summary.vitals.spo2} /><Vital label="Temp" value={summary.vitals.temperature} /></div></div><div className="mt-4 rounded-xl bg-rose-50 border border-rose-200 p-3.5"><p className="text-xs font-bold text-rose-800 flex items-center gap-2"><ShieldAlert className="w-4 h-4" /> AI explanation</p><p className="mt-1.5 text-xs leading-relaxed text-rose-900">{summary.explanation}</p></div></div>

              <div className="grid sm:grid-cols-2 gap-4"><div className="rounded-2xl bg-white border border-slate-200 p-5"><div className="flex items-center gap-2 font-extrabold text-slate-900"><Hospital className="w-4 h-4 text-teal-700" /> Hospital recommendation</div><p className="mt-2 text-sm font-bold text-slate-800">{summary.recommendedHospital}</p><p className="mt-1 text-xs text-slate-500">Emergency-capable destination selected for priority assessment.</p></div><div className="rounded-2xl bg-white border border-slate-200 p-5"><div className="flex items-center gap-2 font-extrabold text-slate-900"><Ambulance className="w-4 h-4 text-rose-600" /> Ambulance routing</div><p className="mt-2 text-sm font-bold text-rose-700">{summary.ambulanceStatus}</p><p className="mt-1 text-xs text-slate-500">ETA: <strong>{summary.ambulanceEta}</strong> • {summary.assignedDoctor}</p></div></div>

              <div className="rounded-2xl bg-white border border-slate-200 p-5"><div className="flex items-center justify-between gap-3"><div><h3 className="font-extrabold text-slate-900 flex items-center gap-2"><CheckCircle2 className="w-5 h-5 text-emerald-600" /> Digital patient summary</h3><p className="text-xs text-slate-500 mt-1">Automatically saved locally and sent to the MediFlow backend when configured.</p></div><button onClick={() => { saveTollFreeCallLocally(session); void persistTollFreeCall(session); setSaved(true); }} className="inline-flex items-center gap-2 px-3 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-xs font-bold text-slate-700"><Save className="w-4 h-4" /> Save</button></div><div className="mt-4 grid grid-cols-2 gap-x-5 gap-y-3 text-xs"><SummaryField label="Name" value={summary.patientName} /><SummaryField label="Location" value={summary.location} /><SummaryField label="Emergency details" value={summary.emergencyDetails} full /><SummaryField label="Risk classification" value={summary.risk} /><SummaryField label="Hospital" value={summary.recommendedHospital} full /><SummaryField label="Routing" value={`${summary.ambulanceStatus} • ${summary.ambulanceEta}`} /><SummaryField label="Clinical desk" value={summary.assignedDoctor} /></div></div>

              <div className="flex flex-col sm:flex-row items-center justify-between gap-3 rounded-2xl bg-slate-900 text-white p-4"><div className="flex items-center gap-2 text-xs text-slate-300"><Clock3 className="w-4 h-4" /><span>Call duration <strong className="text-white font-mono">{String(Math.floor(seconds / 60)).padStart(2, '0')}:{String(seconds % 60).padStart(2, '0')}</strong></span></div><button onClick={endCall} className="inline-flex items-center justify-center gap-2 w-full sm:w-auto px-5 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-sm font-extrabold"><PhoneOff className="w-4 h-4" /> End Call & Save</button></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const VoiceField: React.FC<{ label: string; value: string; onChange: (v: string) => void; onVoice: () => void; onCommit?: () => void; full?: boolean }> = ({ label, value, onChange, onVoice, onCommit, full }) => <label className={full ? 'sm:col-span-2' : ''}><span className="text-[11px] font-bold text-slate-600 uppercase">{label}</span><div className="mt-1 flex gap-2"><input value={value} onChange={e => onChange(e.target.value)} onBlur={onCommit} className="flex-1 rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-teal-100 focus:border-teal-500" /><button type="button" onClick={onVoice} className="shrink-0 rounded-xl bg-teal-50 text-teal-800 px-3 hover:bg-teal-100" title={`Speak ${label}`}><Mic className="w-4 h-4" /></button></div></label>;
const VitalField: React.FC<{ label: string; value: string; onChange: (v: string) => void }> = ({ label, value, onChange }) => <label><span className="text-[11px] font-bold text-slate-600 uppercase">{label}</span><input value={value} onChange={e => onChange(e.target.value)} className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-teal-100 focus:border-teal-500" /></label>;
const Info: React.FC<{ label: string; value: string; icon: React.ReactNode }> = ({ label, value, icon }) => <div className="rounded-xl bg-slate-50 border border-slate-100 p-3"><div className="flex items-center gap-1.5 text-[10px] text-slate-500 font-bold uppercase">{icon}{label}</div><div className="mt-1 font-bold text-slate-800 truncate">{value}</div></div>;
const Vital: React.FC<{ label: string; value: string }> = ({ label, value }) => <div className="rounded-lg bg-slate-50 border border-slate-100 p-2.5"><div className="text-[10px] text-slate-500 font-bold">{label}</div><div className="mt-0.5 text-xs font-extrabold text-slate-800">{value}</div></div>;
const SummaryField: React.FC<{ label: string; value: string; full?: boolean }> = ({ label, value, full }) => <div className={full ? 'col-span-2' : ''}><div className="text-[10px] text-slate-500 font-bold uppercase">{label}</div><div className="mt-0.5 text-xs text-slate-800 font-semibold leading-relaxed">{value}</div></div>;
