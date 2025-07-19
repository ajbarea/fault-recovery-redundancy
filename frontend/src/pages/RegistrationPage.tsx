import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import RegistrationForm from '../components/RegistrationForm';
import LoginForm from '../components/LoginForm';
import type { RegisterResponse } from '../types/RegisterResponse';
import type { LoginResponse } from '../types/LoginResponse';

const RegistrationPage: React.FC = () => {
    const [showRegister, setShowRegister] = useState(true);
    const navigate = useNavigate();

    const handleRegisterSuccess = (data: RegisterResponse) => {
        // Pass the data through navigation state
        navigate('/success', { state: { data } });
    };

    const handleLoginSuccess = (data: LoginResponse) => {
        // Normalize login response to match registration and pass through navigation
        const normalizedData = {
            ...data,
            wasLoggedIn: true,
            wasRegistered: false,
            serverUrl: data.serverUrl || 'rtmp://localhost:1935/live',
        };
        navigate('/success', { state: { data: normalizedData } });
    };

    return (
        <div className="registration-page">
            {showRegister ? (
                <RegistrationForm
                    onRegisterSuccess={handleRegisterSuccess}
                    onSwitchToLogin={() => setShowRegister(false)}
                />
            ) : (
                <LoginForm
                    onLoginSuccess={handleLoginSuccess}
                    onSwitchToRegister={() => setShowRegister(true)}
                />
            )}
        </div>
    );
};

export default RegistrationPage;