import { createContext, useContext, useState, useEffect } from 'react';
import { familyMemberAPI } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('medastrax_token'));
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('medastrax_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [loading, setLoading] = useState(true);
  const [activeProfile, setActiveProfile] = useState(() => {
    const saved = localStorage.getItem('medastrax_active_profile');
    return saved ? JSON.parse(saved) : null;
  });
  const [familyMembers, setFamilyMembers] = useState([]);

  const refreshFamilyMembers = async () => {
    const savedToken = localStorage.getItem('medastrax_token') || token;
    const savedUserString = localStorage.getItem('medastrax_user');
    const savedUser = savedUserString ? JSON.parse(savedUserString) : user;
    if (savedToken && savedUser?.role === 'PATIENT') {
      try {
        const res = await familyMemberAPI.getAll();
        setFamilyMembers(res.data);
      } catch (err) {
        console.error('Failed to load family members', err);
      }
    }
  };

  useEffect(() => {
    // Check for saved auth on mount
    setLoading(false);
  }, []);

  useEffect(() => {
    if (token && user?.role === 'PATIENT') {
      refreshFamilyMembers();
    } else {
      setFamilyMembers([]);
      setActiveProfile(null);
      localStorage.removeItem('medastrax_active_profile');
    }
  }, [token, user]);

  const login = (authResponse) => {
    const { token: jwt, id, name, email, role, phone, avatarUrl } = authResponse;
    const userData = { id, name, email, role, phone, avatarUrl };

    setToken(jwt);
    setUser(userData);
    localStorage.setItem('medastrax_token', jwt);
    localStorage.setItem('medastrax_user', JSON.stringify(userData));
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    setFamilyMembers([]);
    setActiveProfile(null);
    localStorage.removeItem('medastrax_token');
    localStorage.removeItem('medastrax_user');
    localStorage.removeItem('medastrax_active_profile');
  };

  const switchProfile = (profile) => {
    // profile can be null (Myself) or a FamilyMember object
    setActiveProfile(profile);
    if (profile) {
      localStorage.setItem('medastrax_active_profile', JSON.stringify(profile));
    } else {
      localStorage.removeItem('medastrax_active_profile');
    }
  };

  const updateUserAvatar = (avatarUrl) => {
    setUser(prev => {
      if (!prev) return prev;
      const updated = { ...prev, avatarUrl };
      localStorage.setItem('medastrax_user', JSON.stringify(updated));
      return updated;
    });
  };

  const updateUser = (updatedFields) => {
    setUser(prev => {
      if (!prev) return prev;
      const updated = { ...prev, ...updatedFields };
      localStorage.setItem('medastrax_user', JSON.stringify(updated));
      return updated;
    });
  };

  const isAuthenticated = !!token;
  const isPatient = user?.role === 'PATIENT';
  const isDoctor = user?.role === 'DOCTOR';
  const isPharmacy = user?.role === 'PHARMACY';
  const isHospital = user?.role === 'HOSPITAL';
  const isAdmin = user?.role === 'ADMIN';

  return (
    <AuthContext.Provider value={{
      user,
      token,
      loading,
      login,
      logout,
      isAuthenticated,
      isPatient,
      isDoctor,
      isPharmacy,
      isAdmin,
      activeProfile,
      familyMembers,
      switchProfile,
      refreshFamilyMembers,
      updateUserAvatar,
      updateUser
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
