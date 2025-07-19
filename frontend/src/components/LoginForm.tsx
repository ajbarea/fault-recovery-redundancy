import React, { useState } from 'react';
import AuthService from '@/services/AuthService';
import axios from 'axios';
import type { LoginResponse } from '@/types/LoginResponse';

const LoginForm: React.FC<{ onLoginSuccess: (data: LoginResponse) => void, onSwitchToRegister: () => void }> = ({ onLoginSuccess, onSwitchToRegister }) => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setError(null);
        try {
            const response = await AuthService.login(username, password);
            onLoginSuccess(response.data);
        } catch (err) {
            if (axios.isAxiosError(err) && err.response) {
                const errorData = err.response.data;
                if (typeof errorData === 'object' && errorData.error) {
                    setError(errorData.error);
                } else {
                    setError(typeof errorData === 'string' ? errorData : 'Login failed');
                }
            } else {
                setError('Failed to login. An unexpected error occurred.');
            }
            console.error(err);
        }
    };

    return (
        <div className="registration-form-container">
            <form onSubmit={handleSubmit} className="registration-form">
                <h2>Login to Your Account</h2>
                {error && <p className="error">{error}</p>}
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
                <button type="submit">Login</button>
                <div style={{ marginTop: '1rem', textAlign: 'center' }}>
                    <span>Don't have an account? </span>
                    <button type="button" className="secondary-button" onClick={onSwitchToRegister} style={{ padding: '0.5rem 1rem', fontSize: '1rem' }}>
                        Register
                    </button>
                </div>
            </form>
        </div>
    );
};

export default LoginForm;
