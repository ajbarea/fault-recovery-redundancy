import React, { useState } from 'react';
import AuthService from '@/services/AuthService';
import type { RegisterRequest } from '@/types/RegisterRequest';
import axios from 'axios';
import type { RegisterResponse } from '@/types/RegisterResponse';

interface RegistrationFormProps {
    onRegisterSuccess: (data: RegisterResponse) => void;
    onSwitchToLogin: () => void;
}

const RegistrationForm: React.FC<RegistrationFormProps> = ({ onRegisterSuccess, onSwitchToLogin }) => {
    const [email, setEmail] = useState('');
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setError(null);

        if (password !== confirmPassword) {
            setError("Passwords don't match");
            return;
        }

        const user: RegisterRequest = { email, username, password, confirmPassword };

        try {
            const response = await AuthService.register(user);
            onRegisterSuccess(response.data);
        } catch (err) {
            if (axios.isAxiosError(err) && err.response) {
                const errorData = err.response.data;

                if (typeof errorData === 'object' && errorData.error) {
                    setError(errorData.error);
                } else {
                    setError(typeof errorData === 'string' ? errorData : 'Registration failed');
                }
            } else {
                setError('Failed to register. An unexpected error occurred.');
            }
            console.error(err);
        }
    };

    return (
        <div className="registration-form-container">
            <form onSubmit={handleSubmit} className="registration-form">
                <h2 className="form-title">Register for a Stream Key</h2>
                {error && <div className="form-error">{error}</div>}
                <div className="form-group">
                    <label htmlFor="email">Email</label>
                    <input
                        type="email"
                        id="email"
                        className="form-input"
                        value={email}
                        autoComplete='email'
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="username">Username</label>
                    <input
                        type="text"
                        id="username"
                        className="form-input"
                        value={username}
                        autoComplete='username'
                        onChange={(e) => setUsername(e.target.value)}
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="password">Password</label>
                    <input
                        type="password"
                        id="password"
                        className="form-input"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="confirmPassword">Confirm Password</label>
                    <input
                        type="password"
                        id="confirmPassword"
                        className="form-input"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        required
                    />
                </div>

                <button type="submit" className="form-submit">Register</button>
                <div className="auth-switch-container">
                    <span className="auth-switch-text">Already have an account? </span>
                    <button
                        type="button"
                        className="btn-secondary auth-switch-button"
                        onClick={onSwitchToLogin}
                    >
                        Login
                    </button>
                </div>
            </form>
        </div>
    );
};

export default RegistrationForm;



