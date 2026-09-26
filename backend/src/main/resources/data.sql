-- ================= DEPARTMENTS =================
INSERT INTO departments (id, name, description) VALUES
 (1, 'Cardiology', 'Heart and cardiovascular conditions'),
 (2, 'Neurology', 'Brain, spine and nervous system'),
 (3, 'Orthopedics', 'Bones, joints and muscles'),
 (4, 'General Medicine', 'General illness, fever, infections'),
 (5, 'Pulmonology', 'Lungs and breathing conditions'),
 (6, 'Gastroenterology', 'Stomach, digestion and abdominal issues'),
 (7, 'Dermatology', 'Skin, hair and nail conditions'),
 (8, 'ENT', 'Ear, nose and throat'),
 (9, 'Pediatrics', 'Child health'),
 (10, 'Emergency Medicine', 'Critical / trauma / life threatening conditions')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ================= SYMPTOM -> DEPARTMENT MAP =================
-- is_critical = 1 means this symptom keyword should also trigger the emergency flow
INSERT INTO symptom_department_map (id, keyword, department_id, is_critical) VALUES
 (1,  'chest pain', 1, 1),
 (2,  'heart racing', 1, 0),
 (3,  'palpitation', 1, 0),
 (4,  'high blood pressure', 1, 0),
 (5,  'shortness of breath', 5, 1),
 (6,  'difficulty breathing', 5, 1),
 (7,  'cough', 5, 0),
 (8,  'asthma', 5, 0),
 (9,  'headache', 2, 0),
 (10, 'severe headache', 2, 1),
 (11, 'seizure', 2, 1),
 (12, 'numbness', 2, 1),
 (13, 'slurred speech', 2, 1),
 (14, 'stroke', 2, 1),
 (15, 'fracture', 3, 0),
 (16, 'joint pain', 3, 0),
 (17, 'back pain', 3, 0),
 (18, 'bone injury', 3, 1),
 (19, 'fever', 4, 0),
 (20, 'cold', 4, 0),
 (21, 'body ache', 4, 0),
 (22, 'weakness', 4, 0),
 (23, 'stomach pain', 6, 0),
 (24, 'vomiting', 6, 0),
 (25, 'diarrhea', 6, 0),
 (26, 'severe abdominal pain', 6, 1),
 (27, 'blood in stool', 6, 1),
 (28, 'rash', 7, 0),
 (29, 'skin allergy', 7, 0),
 (30, 'itching', 7, 0),
 (31, 'ear pain', 8, 0),
 (32, 'sore throat', 8, 0),
 (33, 'hearing loss', 8, 0),
 (34, 'child fever', 9, 0),
 (35, 'unconscious', 10, 1),
 (36, 'not breathing', 10, 1),
 (37, 'heavy bleeding', 10, 1),
 (38, 'accident', 10, 1),
 (39, 'poisoning', 10, 1)
ON DUPLICATE KEY UPDATE department_id = VALUES(department_id);

-- ================= DOCTORS =================
INSERT INTO doctors (id, name, department_id, qualification, experience_years, fee, photo_url) VALUES
 (1, 'Dr. Ananya Sharma', 1, 'MBBS, MD (Cardiology)', 12, 800.00, NULL),
 (2, 'Dr. Rohan Verma', 1, 'MBBS, DM (Cardiology)', 8, 700.00, NULL),
 (3, 'Dr. Kabir Mehta', 2, 'MBBS, DM (Neurology)', 15, 900.00, NULL),
 (4, 'Dr. Priya Nair', 3, 'MBBS, MS (Ortho)', 10, 600.00, NULL),
 (5, 'Dr. Sameer Khan', 4, 'MBBS, MD (General Medicine)', 6, 400.00, NULL),
 (6, 'Dr. Neha Kapoor', 5, 'MBBS, MD (Pulmonology)', 9, 650.00, NULL),
 (7, 'Dr. Arjun Rao', 6, 'MBBS, MD (Gastro)', 11, 700.00, NULL),
 (8, 'Dr. Ishita Sen', 7, 'MBBS, MD (Dermatology)', 7, 500.00, NULL),
 (9, 'Dr. Vikram Joshi', 8, 'MBBS, MS (ENT)', 13, 550.00, NULL),
 (10, 'Dr. Meera Iyer', 9, 'MBBS, MD (Pediatrics)', 14, 500.00, NULL),
 (11, 'Dr. Farhan Ali', 10, 'MBBS, MD (Emergency Medicine)', 10, 0.00, NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ================= DOCTOR AVAILABILITY =================
INSERT INTO doctor_availability (id, doctor_id, day_of_week, start_time, end_time) VALUES
 (1, 1, 'MONDAY', '09:00:00', '13:00:00'),
 (2, 1, 'WEDNESDAY', '09:00:00', '13:00:00'),
 (3, 1, 'FRIDAY', '16:00:00', '19:00:00'),
 (4, 2, 'TUESDAY', '10:00:00', '14:00:00'),
 (5, 2, 'THURSDAY', '10:00:00', '14:00:00'),
 (6, 3, 'MONDAY', '11:00:00', '15:00:00'),
 (7, 3, 'SATURDAY', '10:00:00', '13:00:00'),
 (8, 4, 'MONDAY', '09:00:00', '17:00:00'),
 (9, 4, 'TUESDAY', '09:00:00', '17:00:00'),
 (10, 5, 'MONDAY', '08:00:00', '20:00:00'),
 (11, 5, 'TUESDAY', '08:00:00', '20:00:00'),
 (12, 5, 'WEDNESDAY', '08:00:00', '20:00:00'),
 (13, 6, 'WEDNESDAY', '09:00:00', '13:00:00'),
 (14, 7, 'THURSDAY', '10:00:00', '14:00:00'),
 (15, 8, 'FRIDAY', '10:00:00', '16:00:00'),
 (16, 9, 'MONDAY', '10:00:00', '14:00:00'),
 (17, 10, 'MONDAY', '09:00:00', '17:00:00'),
 (18, 10, 'WEDNESDAY', '09:00:00', '17:00:00'),
 (19, 11, 'MONDAY', '00:00:00', '23:59:00'),
 (20, 11, 'TUESDAY', '00:00:00', '23:59:00'),
 (21, 11, 'WEDNESDAY', '00:00:00', '23:59:00'),
 (22, 11, 'THURSDAY', '00:00:00', '23:59:00'),
 (23, 11, 'FRIDAY', '00:00:00', '23:59:00'),
 (24, 11, 'SATURDAY', '00:00:00', '23:59:00'),
 (25, 11, 'SUNDAY', '00:00:00', '23:59:00')
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time);

-- ================= HOSPITALS =================
-- Sample coordinates (Kolkata / Ranaghat area, West Bengal) - replace with real data for production
INSERT INTO hospitals (id, name, address, latitude, longitude, phone, emergency_available) VALUES
 (1, 'City Care Multi-Specialty Hospital', 'Station Road, Ranaghat, West Bengal', 23.1785, 88.5620, '03473222111', 1),
 (2, 'Nadia District Hospital', 'Krishnanagar, Nadia, West Bengal', 23.4058, 88.4900, '03472252244', 1),
 (3, 'Apollo Multispeciality Hospitals', 'Salt Lake, Kolkata, West Bengal', 22.5820, 88.4160, '03340660000', 1),
 (4, 'Ranaghat Sub-Divisional Hospital', 'Ranaghat, West Bengal', 23.1770, 88.5590, '03473222555', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ================= AMBULANCES =================
INSERT INTO ambulances (id, driver_name, vehicle_number, contact_number, hospital_id, is_available, latitude, longitude) VALUES
 (1, 'Ramesh Das', 'WB-51-A-1234', '9830011122', 1, 1, 23.1790, 88.5615),
 (2, 'Sanjay Roy', 'WB-51-A-5678', '9830033344', 1, 1, 23.1750, 88.5600),
 (3, 'Abdul Rahim', 'WB-53-B-4321', '9830055566', 2, 0, 23.4050, 88.4890),
 (4, 'Bijoy Mondal', 'WB-19-C-9988', '9830077788', 3, 1, 22.5810, 88.4150),
 (5, 'Tapan Ghosh', 'WB-51-A-3456', '9830099900', 4, 1, 23.1765, 88.5585)
ON DUPLICATE KEY UPDATE driver_name = VALUES(driver_name);
