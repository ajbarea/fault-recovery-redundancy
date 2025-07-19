import React, { useState } from 'react';
import AuthService from '@/services/AuthService';
import type { RegisterRequest } from '@/types/RegisterRequest';
import './RegistrationForm.css';
import axios from 'axios';

const RegistrationForm: React.FC = () => {
    const [email, setEmail] = useState('');
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [streamKey, setStreamKey] = useState<string | null>(null);
    const [registeredUsername, setRegisteredUsername] = useState<string | null>(null);
    const [showToast, setShowToast] = useState(false);


    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setError(null);

        const user: RegisterRequest = { email, username, password, confirmPassword };

        try {
            const response = await AuthService.register(user);
            setStreamKey(response.data.streamKey);
            setRegisteredUsername(username);
        } catch (err) {
            if (axios.isAxiosError(err) && err.response) {
                const errorData = err.response.data;

                // Check if it's a user already exists error
                if (typeof errorData === 'string' &&
                    (errorData.includes('Duplicate entry') ||
                        errorData.includes('already exists') ||
                        errorData.includes('already registered'))) {

                    // User exists, try to log them in with the provided credentials
                    try {
                        const loginResponse = await AuthService.login(username, password);
                        setStreamKey(loginResponse.data.streamKey);
                        setRegisteredUsername(loginResponse.data.username);
                        // Success! User is now logged in
                        return;
                    } catch (loginErr) {
                        // Login failed, probably wrong password
                        setError('User already exists with a different password. Please check your credentials.');
                        console.error('Login attempt failed:', loginErr);
                        return;
                    }
                } else if (typeof errorData === 'object' && errorData.error) {
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

    if (streamKey && registeredUsername) {
        const rtmpUrl = `rtmp://localhost:1935/live`;
        const streamUrl = `http://localhost:9090/live/stream_${registeredUsername}/index.m3u8`;

        const copyToClipboard = (text: string) => {
            navigator.clipboard.writeText(text);
            setShowToast(true);
            setTimeout(() => setShowToast(false), 2000);
        };

        return (
            <div className="registration-form-container">
                {showToast && (
                    <div className="toast">
                        ✅ Copied!
                    </div>
                )}
                <div className="success-container">
                    <div className="success-header">
                        <div className="success-icon">✅</div>
                        <h1 className="success-title">Registration Successful!</h1>
                        <p className="success-subtitle">Your streaming credentials are ready</p>
                    </div>

                    <div className="credentials-section">
                        <div className="credential-item">
                            <label>Username</label>
                            <div className="credential-value">
                                <span className="value-text">{registeredUsername}</span>
                                <button
                                    onClick={() => copyToClipboard(registeredUsername)}
                                    className="copy-btn-small"
                                    title="Copy username"
                                >
                                    📋
                                </button>
                            </div>
                        </div>

                        <div className="credential-item featured">
                            <label>Stream Key</label>
                            <div className="credential-value">
                                <span className="value-text stream-key-text">{streamKey}</span>
                                <button
                                    onClick={() => copyToClipboard(streamKey)}
                                    className="copy-btn-small"
                                    title="Copy stream key"
                                >
                                    📋
                                </button>
                            </div>
                        </div>

                        <div className="credential-item">
                            <label>RTMP URL</label>
                            <div className="credential-value">
                                <span className="value-text">{rtmpUrl}</span>
                                <button
                                    onClick={() => copyToClipboard(rtmpUrl)}
                                    className="copy-btn-small"
                                    title="Copy RTMP URL"
                                >
                                    📋
                                </button>
                            </div>
                        </div>

                        <div className="credential-item">
                            <label>Stream Playback URL</label>
                            <div className="credential-value">
                                <a href={streamUrl} className="value-text stream-url" target="_blank" rel="noopener noreferrer">
                                    {streamUrl}
                                </a>
                                <button
                                    onClick={() => copyToClipboard(streamUrl)}
                                    className="copy-btn-small"
                                    title="Copy stream URL"
                                >
                                    📋
                                </button>
                            </div>
                        </div>
                    </div>

                    <div className="action-buttons">
                        <button
                            onClick={() => copyToClipboard(streamKey)}
                            className="primary-button"
                        >
                            📋 Copy Stream Key
                        </button>
                        <button
                            onClick={() => window.location.reload()}
                            className="secondary-button"
                        >
                            🔄 Register Another User
                        </button>
                    </div>

                    <div className="streaming-instructions">
                        <h3>How to start streaming:</h3>
                        <ol>
                            <li>Open your streaming software (OBS, XSplit, etc.)</li>
                            <li>Use the RTMP URL as your server</li>
                            <li>Use your Stream Key as the stream key</li>
                            <li>Start streaming and share your playback URL!</li>
                        </ol>
                    </div>
                </div>
            </div>
        );
    }

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
            </form>
        </div>
    );
};

export default RegistrationForm;



