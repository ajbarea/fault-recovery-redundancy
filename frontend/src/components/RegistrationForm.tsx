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
                <h2>Register for a Stream Key</h2>
                {error && <p className="error">{error}</p>}
                <div className="form-group">
                    <label htmlFor="email">Email</label>
                    <input
                        type="email"
                        id="email"
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
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        required
                    />
                </div>

                <button type="submit">Register</button>
                <div style={{ marginTop: '1rem', textAlign: 'center' }}>
                    <span>Already have an account? </span>
                    <button
                        type="button"
                        className="secondary-button"
                        onClick={onSwitchToLogin}
                        style={{ padding: '0.5rem 1rem', fontSize: '1rem' }}
                    >
                        Login
                    </button>
                </div>
            </form>
        </div>
    );
};

export default RegistrationForm;



