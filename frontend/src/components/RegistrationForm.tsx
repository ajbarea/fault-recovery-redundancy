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
        const streamUrl = `http://localhost:9090/live/stream_${registeredUsername}/index.m3u8`;
        return (
            <div className="registration-form-container">
                <div className="registration-form">
                    <h2>Registration Successful!</h2>
                    <p>Here is your stream key:</p>
                    <div className="stream-key-container">
                        <p>Username: {registeredUsername}</p>
                        <p>Stream Key: {streamKey}</p>
                        <p>RTMP URL: rtmp://localhost:1935/live</p>
                        <p>Your stream URL (after starting): <a href={streamUrl}>{streamUrl}</a></p>
                        <button
                            onClick={() => navigator.clipboard.writeText(streamKey)}
                            className="copy-button"
                        >
                            Copy Stream Key
                        </button>
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



