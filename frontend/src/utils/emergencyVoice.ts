export type VoiceLanguage = 'en-IN' | 'hi-IN';

const getSynthesis = () => typeof window !== 'undefined' ? window.speechSynthesis : null;

export function speakEmergency(text: string, language: VoiceLanguage = 'en-IN') {
  const synthesis = getSynthesis();
  if (!synthesis || !text.trim()) return false;
  synthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = language;
  utterance.rate = 0.92;
  utterance.pitch = 1;
  utterance.volume = 1;
  const voices = synthesis.getVoices();
  const preferred = voices.find(v => v.lang.toLowerCase() === language.toLowerCase())
    || voices.find(v => v.lang.toLowerCase().startsWith(language.slice(0,2)))
    || voices.find(v => v.lang.toLowerCase().includes('en-in'));
  if (preferred) utterance.voice = preferred;
  synthesis.speak(utterance);
  return true;
}

export function stopEmergencyVoice() {
  getSynthesis()?.cancel();
}

export function hasSpeechRecognition() {
  if (typeof window === 'undefined') return false;
  return Boolean((window as any).SpeechRecognition || (window as any).webkitSpeechRecognition);
}

export function listenOnce(onText: (text: string) => void, language: VoiceLanguage = 'en-IN') {
  if (typeof window === 'undefined') return false;
  const Recognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
  if (!Recognition) return false;
  const recognition = new Recognition();
  recognition.lang = language;
  recognition.interimResults = false;
  recognition.continuous = false;
  recognition.maxAlternatives = 1;
  recognition.onresult = (event: any) => onText(event.results?.[0]?.[0]?.transcript || '');
  recognition.start();
  return true;
}
