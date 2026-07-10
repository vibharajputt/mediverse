import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://mediverse-ke9x.onrender.com/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('medastrax_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthEndpoint = error.config?.url?.includes('/auth/login') || 
                           error.config?.url?.includes('/auth/google-login') || 
                           error.config?.url?.includes('/auth/signup');
    
    if ((error.response?.status === 401 || error.response?.status === 403) && !isAuthEndpoint) {
      localStorage.removeItem('medastrax_token');
      localStorage.removeItem('medastrax_user');
      localStorage.removeItem('medastrax_active_profile');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  signup: (data) => api.post('/auth/signup', data),
  login: (data) => api.post('/auth/login', data),
  googleLogin: (email) => api.post('/auth/google-login', { email }),
  updateAvatar: (avatarUrl) => api.put('/auth/profile/avatar', { avatarUrl }),
  getProfile: () => api.get('/auth/profile'),
  updateProfile: (data) => api.put('/auth/profile', data),
  verifyUpi: (upiId) => api.get(`/auth/verify-upi?upiId=${upiId}`),
  resetPassword: (email, newPassword) => api.post('/auth/reset-password', { email, newPassword }),
  verifyLicense: (licenseNo) => api.get(`/auth/verify-license?licenseNo=${licenseNo}`),
  verifyPharmacyLicense: (licenseNo) => api.get(`/auth/verify-pharmacy-license?licenseNo=${licenseNo}`),
  verifyLabLicense: (licenseNo) => api.get(`/auth/verify-lab-license?licenseNo=${licenseNo}`),
  getPatientProfileForDoctor: (patientId) => api.get(`/auth/patient/${patientId}`),
  getDoctors: () => api.get('/auth/doctors'),
  getPatients: () => api.get('/auth/patients'),
  getBookings: () => api.get('/auth/bookings'),
  getOrders: () => api.get('/auth/orders'),
  getLabBookings: () => api.get('/auth/lab-bookings'),
};

// OTP API
export const otpAPI = {
  sendOtp: (identifier, type) => api.post('/auth/otp/send', { identifier, type }),
  verifyOtp: (identifier, type, otp) => api.post('/auth/otp/verify', { identifier, type, otp }),
  checkStatus: (identifier, type) => api.get(`/auth/otp/status?identifier=${identifier}&type=${type}`),
};

// Hospital API
export const hospitalAPI = {
  getAll: () => api.get('/hospitals'),
  getById: (id) => api.get(`/hospitals/${id}`),
  search: (query) => api.get(`/hospitals/search?query=${query}`),
  getByDoctor: (doctorId) => api.get(`/hospitals/doctor/${doctorId}`),
  create: (data) => api.post('/hospitals', data),
  update: (id, data) => api.put(`/hospitals/${id}`, data),
  updateBeds: (id, beds) => api.put(`/hospitals/${id}/beds?availableBeds=${beds}`),
  getDoctors: (id) => api.get(`/hospitals/${id}/doctors`),
  verify: (id, verified) => api.put(`/hospitals/${id}/verify?verified=${verified}`),
};

export const bookingAPI = {
  create: (data) => api.post('/bookings', data),
  getPatientBookings: (familyMemberId) => api.get(`/bookings/patient${familyMemberId ? `?familyMemberId=${familyMemberId}` : ''}`),
  getDoctorBookings: () => api.get('/bookings/doctor'),
  getById: (id) => api.get(`/bookings/${id}`),
  updateStatus: (id, status) => api.put(`/bookings/${id}/status?status=${status}`),
  getAvailableSlots: (doctorId, date) => api.get(`/bookings/slots?doctorId=${doctorId}&date=${date}`),
  updateMeetingLink: (id, meetingLink) => api.put(`/bookings/${id}/meeting-link?meetingLink=${encodeURIComponent(meetingLink)}`),
  updateAiReport: (id, aiReport) => api.put(`/bookings/${id}/ai-report`, { aiReport }),
  reschedule: (id, date, timeSlot) => api.put(`/bookings/${id}/reschedule?date=${date}&timeSlot=${encodeURIComponent(timeSlot)}`),
};

// Prescription API
export const prescriptionAPI = {
  create: (data) => api.post('/prescriptions', data),
  getPatientPrescriptions: (familyMemberId) => api.get(`/prescriptions/patient${familyMemberId ? `?familyMemberId=${familyMemberId}` : ''}`),
  getDoctorPrescriptions: () => api.get('/prescriptions/doctor'),
  getById: (id) => api.get(`/prescriptions/${id}`),
  analyze: (id) => api.get(`/prescriptions/${id}/analyze`),
  analyzeRaw: (data) => api.post('/prescriptions/analyze-raw', data),
  analyzeReportDocument: (data) => api.post('/prescriptions/analyze-document', data),
  getPharmacyQueue: () => api.get('/prescriptions/pharmacy-queue'),
  uploadReport: (id, reportUrl) => api.put(`/prescriptions/${id}/upload-report`, { reportUrl }),
};

// Family Member API
export const familyMemberAPI = {
  add: (data) => api.post('/family-members', data),
  getAll: () => api.get('/family-members'),
  delete: (id) => api.delete(`/family-members/${id}`),
};

// Pharmacy API
export const pharmacyAPI = {
  setPrices: (data) => api.post('/pharmacy/prices', data),
  getMedicines: () => api.get('/pharmacy/medicines'),
  getForPrescription: (prescriptionId) => api.get(`/pharmacy/prescription/${prescriptionId}`),
  getAll: () => api.get('/pharmacy/all'),
  updateProfile: (data) => api.put('/pharmacy/profile', data),
  createOrder: (data) => api.post('/orders', data),
  getOrdersForPharmacy: (pharmacyName) => api.get(`/orders/pharmacy?pharmacyName=${encodeURIComponent(pharmacyName)}`),
  updateOrderStatus: (orderId, status) => api.put(`/orders/${orderId}/status`, { status }),
};

// Lab API
export const labAPI = {
  getAll: () => api.get('/labs/all'),
  createBooking: (data) => api.post('/labs/bookings', data),
  getPatientBookings: () => api.get('/labs/bookings/patient'),
  updateBookingStatus: (id, status) => api.put(`/labs/bookings/${id}/status`, { status }),
  getLabBookings: () => api.get('/labs/bookings/lab'),
};

// Payment API
export const paymentAPI = {
  createOrder: (data) => api.post('/payments/order', data),
  verifyPayment: (data) => api.post('/payments/verify', data),
};

// Notifications API
export const notificationAPI = {
  getNotifications: ({ role, userId } = {}) => {
    const params = [];
    if (role) params.push(`role=${encodeURIComponent(role)}`);
    if (userId) params.push(`userId=${encodeURIComponent(userId)}`);
    const q = params.length ? `?${params.join('&')}` : '';
    return api.get(`/notifications${q}`);
  }
};

// File Upload API
export const fileAPI = {
  upload: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  }
};

// AI API
export const aiAPI = {
  chat: (message, sessionId) => api.post('/ai/chat', { message, sessionId }),
  resetChat: (sessionId) => api.post('/ai/chat/reset', { sessionId }),
  queryChat: (message, sessionId) => api.post('/ai/query-chat', { message, sessionId }),
  resetQueryChat: (sessionId) => api.post('/ai/query-chat/reset', { sessionId }),
  analyzeConsultation: (transcript, patientName, doctorName) => api.post('/ai/analyze-consultation', { transcript, patientName, doctorName }),
  getCarePlan: () => api.get('/ai/care-plan'),
  compareReports: (previousReport, currentReport) => api.post('/ai/compare-reports', { previousReport, currentReport }),
  analyzePatientReports: () => api.post('/ai/analyze-reports'),
  analyzeBodySymptoms: (data) => api.post('/ai/analyze-body-symptoms', data),
  assessSkinCare: (data) => api.post('/ai/skin-assessment', data),
};

// Rewards API
export const rewardsAPI = {
  updateChecklist: (data) => api.post('/rewards/checklist', data),
  getLeaderboard: () => api.get('/rewards/leaderboard'),
};

// Emergency SOS API
export const emergencyAPI = {
  triggerSOS: (data) => api.post('/sos', data),
};

export default api;
