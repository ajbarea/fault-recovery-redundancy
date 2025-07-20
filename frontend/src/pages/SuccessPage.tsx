import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useToast } from '../hooks/useToast';
import StreamKeyDisplay from '../components/StreamKeyDisplay';
import type { CredentialResponse } from '../types/CredentialResponse';

const SuccessPage: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const { show, message, triggerToast } = useToast();

    // Get data from navigation state
    const data = location.state?.data as CredentialResponse;

    // If no data, redirect back to registration
    if (!data) {
        navigate('/');
        return null;
    }

    const copyToClipboard = (text: string) => {
        navigator.clipboard.writeText(text);
        triggerToast('✅ Copied!');
    };

    const handleRegisterAnother = () => {
        navigate('/');
    };

    return (
        <div className="registration-form-container">
            {show && (
                <div className="toast">{message}</div>
            )}
            <div className="success-container">
                <div className="success-header">
                    <div className="success-icon">{data.wasLoggedIn ? '🔐' : '✅'}</div>
                    <h1 className="success-title">
                        {data.wasLoggedIn ? 'Welcome Back!' : 'Registration Successful!'}
                    </h1>
                    <p className="success-subtitle">
                        {data.wasLoggedIn
                            ? 'You were automatically logged in with your existing account'
                            : 'Your streaming credentials are ready'}
                    </p>
                </div>

                <StreamKeyDisplay
                    streamKey={data.streamKey}
                    username={data.username}
                    rtmpUrl={data.serverUrl || 'rtmp://localhost:1935/live'}
                />

                <div className="credential-item">
                    <label>Stream Playback URL</label>
                    <div className="credential-value">
                        <a href={data.streamUrl} className="value-text stream-url" target="_blank" rel="noopener noreferrer">{data.streamUrl}</a>
                        <button onClick={() => copyToClipboard(data.streamUrl)} className="copy-btn-small" title="Copy stream URL">📋</button>
                    </div>
                </div>

                <div className="action-buttons">
                    <button onClick={handleRegisterAnother} className="secondary-button">🔄 Register or Login Another User</button>
                </div>
            </div>
        </div>
    );
};

export default SuccessPage;