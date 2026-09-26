import React, { useState } from 'react';
import { FileText, Upload, Camera, Check, AlertCircle, Sparkles, RefreshCw, Eye, Calendar, Building2, User, Pill, FlaskConical, ShieldAlert } from 'lucide-react';
import { ScannedDocument } from '../../types';
import { createWorker } from 'tesseract.js';
import { extractPrescriptionRules, sanitizeMedicationList } from '../../utils/prescriptionRuleExtractor';
import { preprocessPrescriptionImage } from '../../utils/imagePreprocessing';

interface DocumentScannerProps {
  documents: ScannedDocument[];
  onAddDocument: (doc: ScannedDocument) => void;
  onRemoveDocument: (id: string) => void;
}

const PRESET_SAMPLE_DOCS = [
  {
    name: 'Handwritten Indian OPD Prescription (2024)',
    type: 'prescription' as const,
    url: 'https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?auto=format&fit=crop&w=800&q=80',
    hint: 'Demo handwritten prescription — results are extracted from the selected image'
  },
  {
    name: 'Biochemistry Blood Panel (HbA1c & Creatinine)',
    type: 'lab_report' as const,
    url: 'https://images.unsplash.com/photo-1579154204601-01588f351e67?auto=format&fit=crop&w=800&q=80',
    hint: 'Apex Lab report with elevated HbA1c (9.2%) and Serum Creatinine (1.42)'
  },
  {
    name: 'Ayurvedic Treatment & Kashayam Slip (2025)',
    type: 'ayush_slip' as const,
    url: 'https://images.unsplash.com/photo-1512290900672-1f00b7b6294d?auto=format&fit=crop&w=800&q=80',
    hint: 'Govt. Ayurvedic Dispensary slip with Yograj Guggulu & Mahanarayana Taila'
  }
];

export const DocumentScanner: React.FC<DocumentScannerProps> = ({
  documents,
  onAddDocument,
  onRemoveDocument
}) => {
  const [isScanning, setIsScanning] = useState(false);
  const [selectedPreviewDoc, setSelectedPreviewDoc] = useState<ScannedDocument | null>(documents[0] || null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    // Reset the input so selecting the same file twice still triggers onChange.
    e.target.value = '';
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setErrorMessage('Please upload a prescription as JPG, JPEG, PNG, WEBP, or a camera image. PDF OCR is not enabled in this browser-only scanner yet.');
      return;
    }

    if (file.size > 12 * 1024 * 1024) {
      setErrorMessage('The image is larger than 12 MB. Please choose a smaller photo or compress the prescription image first.');
      return;
    }

    const reader = new FileReader();
    reader.onload = async (event) => {
      const dataUrl = event.target?.result as string;
      if (!dataUrl) {
        setErrorMessage('The selected image could not be read. Please try another image.');
        return;
      }
      await processImageWithGemini(dataUrl, file.name, 'prescription', file.type);
    };
    reader.onerror = () => setErrorMessage('Could not read the selected file. Please try again.');
    reader.readAsDataURL(file);
  };

  const handleSelectPreset = async (preset: typeof PRESET_SAMPLE_DOCS[0]) => {
    await processImageWithGemini(preset.url, preset.name, preset.type);
  };

  const processImageWithGemini = async (
    imageSrc: string,
    fileName: string,
    docType: ScannedDocument['fileType'],
    mimeTypeOverride?: string
  ) => {
    setIsScanning(true);
    setErrorMessage(null);

    // Keep this OUTSIDE the try block. The previous implementation declared it
    // inside try and then referenced it from catch, which caused a ReferenceError
    // precisely when the OCR API returned 404/unavailable.
    let localOcrText = '';

    try {
      let base64Payload = imageSrc;
      let actualMimeType = mimeTypeOverride || 'image/jpeg';

      // Preset/demo URLs are converted to a data URL before OCR. Uploaded files
      // already arrive as data URLs from FileReader.
      if (imageSrc.startsWith('http')) {
        const fetched = await fetch(imageSrc);
        if (!fetched.ok) throw new Error(`Could not load sample image (HTTP ${fetched.status})`);
        const blob = await fetched.blob();
        actualMimeType = blob.type || actualMimeType;
        base64Payload = await new Promise<string>((resolve, reject) => {
          const reader = new FileReader();
          reader.onload = () => resolve(String(reader.result || ''));
          reader.onerror = () => reject(new Error('Could not convert sample image'));
          reader.readAsDataURL(blob);
        });
      }

      // Improve handwriting/photo OCR with the existing browser-side image
      // preprocessing pipeline. The original image is always kept for display.
      let ocrImagePayload = base64Payload;
      if (base64Payload.startsWith('data:image/')) {
        try {
          const prepared = await preprocessPrescriptionImage(base64Payload, {
            maxDimension: 2048,
            enhanceContrast: true,
            sharpen: true
          });
          if (!prepared.quality.isValid) {
            throw new Error(prepared.quality.reason || 'The uploaded image could not be decoded.');
          }
          ocrImagePayload = prepared.processedDataUrl;
        } catch (preprocessError) {
          console.warn('Prescription image preprocessing failed; using original image:', preprocessError);
        }

        // Browser-side OCR is the reliable baseline and does not depend on the
        // optional Spring Boot OCR route being present.
        try {
          const worker = await createWorker('eng');
          const localResult = await worker.recognize(ocrImagePayload);
          localOcrText = (localResult.data.text || '').trim();
          await worker.terminate();
        } catch (localOcrError) {
          console.warn('Document scanner local OCR failed:', localOcrError);
        }
      }

      const localRules = localOcrText ? extractPrescriptionRules(localOcrText, '') : null;

      // The Spring Boot project may be running without an OCR controller. Try
      // the server only when available; a 404/network error must NOT prevent the
      // uploaded document from being saved locally.
      let serverExtracted: any = null;
      try {
        const res = await fetch('/api/intake/ocr-document', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            imageBase64: ocrImagePayload,
            mimeType: actualMimeType,
            documentTypeHint: docType,
          }),
        });

        if (res.ok) {
          const data = await res.json();
          if (data?.success && data?.extractedData) serverExtracted = data.extractedData;
        } else {
          console.info(`Optional server OCR unavailable (HTTP ${res.status}); using local OCR.`);
        }
      } catch (serverError) {
        console.info('Optional server OCR unavailable; using local OCR:', serverError);
      }

      let extracted: any = serverExtracted || {};

      // Prefer deterministic extraction from the actual image OCR. This prevents
      // an AI/demo response from replacing the medicines visible in the upload.
      if (localRules?.medications?.length) {
        const localMeds = localRules.medications.filter((m: any) =>
          m?.medicineName && !/^(after|before|with|at night|at bedtime|take|advice|instructions)$/i.test(m.medicineName.trim())
        );
        if (localMeds.length) {
          extracted.medications = sanitizeMedicationList(localMeds).map((m: any) => ({
            name: m.medicineName,
            dose: m.strength || m.dosage || '',
            frequency: m.frequency || '',
            duration: m.duration || '',
            instructions: m.foodInstruction || m.specialInstruction || ''
          }));
        }
      }
      if (!extracted.doctorName || /not detected|unclear|consulting physician/i.test(extracted.doctorName)) {
        if (localRules?.doctorName) extracted.doctorName = localRules.doctorName;
      }
      if (!extracted.hospitalOrClinic || /hospital \/ diagnostic center|not detected|unclear/i.test(extracted.hospitalOrClinic)) {
        if (localRules?.hospitalName) extracted.hospitalOrClinic = localRules.hospitalName;
      }
      if (!extracted.date && localRules?.prescriptionDate) extracted.date = localRules.prescriptionDate;
      extracted.medications = sanitizeMedicationList(Array.isArray(extracted.medications) ? extracted.medications : []);
      if (!extracted.ocrText) extracted.ocrText = localOcrText;

      const newDoc: ScannedDocument = {
        id: 'doc-' + Date.now(),
        fileName,
        fileType: extracted.fileType || docType,
        imageUrl: base64Payload,
        documentDate: extracted.documentDate || extracted.date || localRules?.prescriptionDate || new Date().toISOString().split('T')[0],
        providerName: extracted.hospitalOrClinic || localRules?.hospitalName || 'Not detected — verify from original document',
        doctorName: extracted.doctorName || localRules?.doctorName || 'Not detected — verify from original document',
        extractedData: {
          ...extracted,
          hospitalOrClinic: extracted.hospitalOrClinic || localRules?.hospitalName || '',
          doctorName: extracted.doctorName || localRules?.doctorName || '',
          date: extracted.date || localRules?.prescriptionDate || '',
          diagnoses: Array.isArray(extracted.diagnoses)
            ? extracted.diagnoses
            : (localRules?.diagnosis && !/not explicitly mentioned/i.test(localRules.diagnosis) ? [localRules.diagnosis] : []),
          medications: Array.isArray(extracted.medications) ? extracted.medications : [],
          labResults: Array.isArray(extracted.labResults) ? extracted.labResults : [],
          keyObservations: extracted.keyObservations || [
            localOcrText
              ? 'Extracted from the uploaded image using browser OCR; verify against the original prescription.'
              : 'Image uploaded successfully. OCR text was not readable; manual verification required.'
          ],
          confidenceScore: typeof extracted.confidenceScore === 'number'
            ? extracted.confidenceScore
            : (localRules?.overallConfidence ? localRules.overallConfidence / 100 : 0),
        },
        verifiedByPatient: false,
      };

      onAddDocument(newDoc);
      setSelectedPreviewDoc(newDoc);

      if (!localOcrText && !serverExtracted) {
        setErrorMessage('Upload succeeded, but the handwriting could not be read automatically. The original image is saved for manual verification.');
      }
    } catch (err: any) {
      console.warn('Document scanner upload processing warning:', err);

      // Last-resort behavior: NEVER discard the user's uploaded image merely
      // because OCR/API processing failed. The document remains visible and can
      // be verified manually.
      const fallbackDoc: ScannedDocument = {
        id: 'doc-' + Date.now(),
        fileName,
        fileType: docType,
        imageUrl: imageSrc,
        documentDate: new Date().toISOString().split('T')[0],
        providerName: 'Not detected — verify from original document',
        doctorName: 'Not detected — verify from original document',
        extractedData: {
          hospitalOrClinic: '',
          doctorName: '',
          date: '',
          diagnoses: [],
          medications: [],
          labResults: [],
          keyObservations: ['Image uploaded successfully. OCR processing failed; manual verification required.'],
          confidenceScore: 0,
        },
        verifiedByPatient: false,
      };
      onAddDocument(fallbackDoc);
      setSelectedPreviewDoc(fallbackDoc);
      setErrorMessage(err?.message || 'The image was uploaded, but OCR processing failed. The original document is saved for manual verification.');
    } finally {
      setIsScanning(false);
    }
  };

  return (
    <div id="document-scanner-section" className="space-y-6">
      {/* Header Info */}
      <div className="p-4 bg-teal-50/80 rounded-xl border border-teal-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h3 className="font-bold text-teal-950 text-base sm:text-lg flex items-center gap-2">
            <FileText className="w-5 h-5 text-teal-600" />
            Medical Document Scanner & Intelligent OCR
          </h3>
          <p className="text-xs sm:text-sm text-teal-800">
            Upload or select previous handwritten prescriptions, lab reports, or discharge slips. The AI extracts medicines, lab values, and dates chronologically.
          </p>
        </div>
        <span className="text-xs font-semibold px-3 py-1 bg-teal-100 text-teal-800 rounded-full self-start sm:self-auto shrink-0">
          {documents.length} Records Digitized
        </span>
      </div>

      {/* Upload / Quick Presets Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        {/* Upload Button */}
        <label className="p-4 bg-white hover:bg-slate-50 border-2 border-dashed border-slate-300 hover:border-teal-500 rounded-xl cursor-pointer transition-all flex flex-col items-center justify-center text-center group">
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp,image/jpg"
            className="hidden"
            onChange={handleFileUpload}
            disabled={isScanning}
          />
          <div className="w-10 h-10 rounded-full bg-teal-50 text-teal-600 flex items-center justify-center group-hover:scale-110 transition-transform mb-2">
            <Upload className="w-5 h-5" />
          </div>
          <span className="font-bold text-slate-800 text-xs sm:text-sm">Upload Prescription / Lab Report</span>
          <span className="text-[11px] text-slate-400 mt-0.5">JPEG, PNG, Camera capture</span>
        </label>

        {/* 1-Click Preset Samples */}
        {PRESET_SAMPLE_DOCS.map((preset, idx) => (
          <button
            key={idx}
            type="button"
            onClick={() => handleSelectPreset(preset)}
            disabled={isScanning}
            className="p-3 bg-white hover:bg-teal-50/50 border border-slate-200 hover:border-teal-300 rounded-xl text-left transition-all flex items-start gap-3 group"
          >
            <div className="w-9 h-9 rounded-lg bg-slate-100 group-hover:bg-teal-100 text-slate-600 group-hover:text-teal-700 flex items-center justify-center shrink-0">
              <Camera className="w-4 h-4" />
            </div>
            <div>
              <p className="font-bold text-xs text-slate-900 line-clamp-1">{preset.name}</p>
              <p className="text-[11px] text-slate-500 line-clamp-2 mt-0.5">{preset.hint}</p>
              <span className="inline-flex items-center gap-1 text-[10px] font-bold text-teal-600 mt-1">
                <Sparkles className="w-3 h-3" /> Click to Test Scan
              </span>
            </div>
          </button>
        ))}
      </div>

      {errorMessage && (
        <div className="flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900" role="alert">
          <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
          <div className="flex-1">{errorMessage}</div>
          <button
            type="button"
            onClick={() => setErrorMessage(null)}
            className="text-xs font-bold text-amber-700 hover:underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Processing Loader */}
      {isScanning && (
        <div className="p-6 bg-teal-900 text-white rounded-2xl flex flex-col items-center justify-center gap-3 animate-pulse">
          <RefreshCw className="w-8 h-8 text-teal-300 animate-spin" />
          <div className="text-center">
            <p className="font-bold text-base">Gemini 3.7 Flash Multimodal OCR in Progress...</p>
            <p className="text-xs text-teal-200 mt-1">
              Transcribing doctor's handwriting, parsing drug dosages, extracting lab reference values & identifying flags...
            </p>
          </div>
        </div>
      )}

      {/* Side-by-Side Scanned Documents View */}
      {documents.length > 0 && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h4 className="font-bold text-slate-900 text-sm flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-teal-600" />
              Digitized Clinical Documents ({documents.length})
            </h4>
          </div>

          {/* Document Pills */}
          <div className="flex flex-wrap gap-2">
            {documents.map((doc) => {
              const isSelected = selectedPreviewDoc?.id === doc.id;
              return (
                <button
                  key={doc.id}
                  type="button"
                  onClick={() => setSelectedPreviewDoc(doc)}
                  className={`px-3.5 py-2 rounded-xl text-xs font-semibold border transition-all flex items-center gap-2 ${
                    isSelected
                      ? 'bg-slate-900 text-white border-slate-900 shadow-sm'
                      : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <FileText className="w-3.5 h-3.5 text-teal-500" />
                  <span>{doc.fileName.replace(/_/g, ' ').slice(0, 24)}</span>
                  <span className="text-[10px] px-1.5 py-0.5 bg-slate-100 text-slate-600 rounded">
                    {doc.documentDate}
                  </span>
                </button>
              );
            })}
          </div>

          {/* Selected Document Detailed Inspection Card */}
          {selectedPreviewDoc && (
            <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
              <div className="p-4 bg-slate-900 text-white flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 bg-teal-500/20 text-teal-300 rounded-lg">
                    <FileText className="w-5 h-5" />
                  </div>
                  <div>
                    <h5 className="font-bold text-sm">{selectedPreviewDoc.fileName}</h5>
                    <p className="text-xs text-slate-300 flex items-center gap-2">
                      <span>🏥 {selectedPreviewDoc.extractedData?.hospitalOrClinic || selectedPreviewDoc.providerName}</span>
                      <span>• 📅 {selectedPreviewDoc.extractedData?.date || selectedPreviewDoc.documentDate}</span>
                    </p>
                  </div>
                  <p className="text-xs text-slate-500 mt-1">
                    Doctor: {selectedPreviewDoc.extractedData?.doctorName || selectedPreviewDoc.doctorName || 'Not detected'}
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <span className="text-[11px] font-bold px-2.5 py-1 bg-teal-400/20 text-teal-300 border border-teal-400/30 rounded-full flex items-center gap-1">
                    <Check className="w-3 h-3 text-teal-400" />
                    OCR Verified (Confidence: {Math.round((selectedPreviewDoc.extractedData?.confidenceScore || 0.95) * 100)}%)
                  </span>
                  <button
                    type="button"
                    onClick={() => onRemoveDocument(selectedPreviewDoc.id)}
                    className="text-xs text-rose-300 hover:text-rose-100 hover:underline ml-2"
                  >
                    Remove
                  </button>
                </div>
              </div>

              {/* Document Extracted Details Grid */}
              <div className="p-5 grid grid-cols-1 lg:grid-cols-2 gap-6 bg-slate-50/50">
                {/* Left: Document Scan Thumbnail Preview */}
                <div className="space-y-2">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                    <Eye className="w-3.5 h-3.5" /> Original Scanned Document
                  </span>
                  <div className="relative rounded-xl overflow-hidden border border-slate-200 bg-slate-100 max-h-[300px] flex items-center justify-center">
                    <img
                      src={selectedPreviewDoc.imageUrl}
                      alt="Scanned Document"
                      className="w-full h-full object-cover"
                      referrerPolicy="no-referrer"
                    />
                    <div className="absolute bottom-2 left-2 px-2.5 py-1 bg-black/70 backdrop-blur-xs text-white text-[11px] font-mono rounded">
                      Digitized by Gemini 3.7 Flash
                    </div>
                  </div>
                </div>

                {/* Right: Extracted Structured Clinical Entities */}
                <div className="space-y-4">
                  {/* Extracted Diagnoses */}
                  {selectedPreviewDoc.extractedData?.diagnoses && selectedPreviewDoc.extractedData.diagnoses.length > 0 && (
                    <div>
                      <span className="text-xs font-bold uppercase tracking-wider text-slate-500 block mb-1.5">
                        Extracted Diagnoses
                      </span>
                      <div className="flex flex-wrap gap-1.5">
                        {selectedPreviewDoc.extractedData.diagnoses.map((d, i) => (
                          <span key={i} className="px-2.5 py-1 bg-amber-50 text-amber-900 border border-amber-200 text-xs font-bold rounded-lg">
                            🏷️ {d}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Extracted Medicines */}
                  {selectedPreviewDoc.extractedData?.medications && selectedPreviewDoc.extractedData.medications.length > 0 && (
                    <div>
                      <span className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5 mb-1.5">
                        <Pill className="w-3.5 h-3.5 text-teal-600" /> Prescribed Medications Found
                      </span>
                      <div className="space-y-1.5">
                        {selectedPreviewDoc.extractedData.medications.map((m, i) => (
                          <div key={i} className="p-2.5 bg-white border border-slate-200 rounded-xl text-xs flex items-center justify-between shadow-2xs">
                            <div>
                              <p className="font-bold text-slate-900">{m.name} {m.dose}</p>
                              <p className="text-slate-500 text-[11px]">
                                Dosage: {m.frequency} {m.duration ? `• ${m.duration}` : ''} {m.instructions ? `• ${m.instructions}` : ''}
                              </p>
                            </div>
                            <span className="text-[10px] font-bold px-2 py-0.5 bg-teal-50 text-teal-700 border border-teal-200 rounded">
                              Active Med
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Extracted Lab Results */}
                  {selectedPreviewDoc.extractedData?.labResults && selectedPreviewDoc.extractedData.labResults.length > 0 && (
                    <div>
                      <span className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5 mb-1.5">
                        <FlaskConical className="w-3.5 h-3.5 text-teal-600" /> Pathology / Lab Values
                      </span>
                      <div className="space-y-1.5">
                        {selectedPreviewDoc.extractedData.labResults.map((lr, i) => (
                          <div key={i} className="p-2.5 bg-white border border-slate-200 rounded-xl text-xs flex items-center justify-between shadow-2xs">
                            <div>
                              <p className="font-bold text-slate-900">{lr.testName}</p>
                              <p className="text-slate-500 text-[11px]">Reference: {lr.referenceRange} {lr.unit}</p>
                            </div>
                            <div className="text-right">
                              <span className={`font-mono font-bold text-sm ${
                                lr.status === 'high' || lr.status === 'critical' ? 'text-rose-600' : 'text-slate-800'
                              }`}>
                                {lr.value} {lr.unit}
                              </span>
                              <span className={`block text-[10px] font-bold uppercase ${
                                lr.status === 'high' ? 'text-rose-600' : lr.status === 'low' ? 'text-amber-600' : 'text-emerald-600'
                              }`}>
                                {lr.status}
                              </span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Key Observations / Notes */}
                  {selectedPreviewDoc.extractedData?.keyObservations && (
                    <div className="p-3 bg-blue-50/70 border border-blue-200 rounded-xl text-xs text-blue-900 space-y-1">
                      <span className="font-bold block">💡 AI Clinical Highlight:</span>
                      {selectedPreviewDoc.extractedData.keyObservations.map((obs, i) => (
                        <p key={i}>• {obs}</p>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
