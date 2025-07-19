import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useToast } from '../hooks/useToast';
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
                <div className="credentials-section">
                    <div className="credential-item">
                        <label>Username</label>
                        <div className="credential-value">
                            <span className="value-text">{data.username}</span>
                            <button onClick={() => copyToClipboard(data.username)} className="copy-btn-small" title="Copy username">📋</button>
                        </div>
                    </div>
                    <div className="credential-item featured">
                        <label>Stream Key</label>
                        <div className="credential-value">
                            <span className="value-text stream-key-text">{data.streamKey}</span>
                            <button onClick={() => copyToClipboard(data.streamKey)} className="copy-btn-small" title="Copy stream key">📋</button>
                        </div>
                    </div>
                    <div className="credential-item">
                        <label>RTMP URL</label>
                        <div className="credential-value">
                            <span className="value-text">{data.serverUrl || 'rtmp://localhost:1935/live'}</span>
                            <button onClick={() => copyToClipboard(data.serverUrl || 'rtmp://localhost:1935/live')} className="copy-btn-small" title="Copy RTMP URL">📋</button>
                        </div>
                    </div>
                    <div className="credential-item">
                        <label>Stream Playback URL</label>
                        <div className="credential-value">
                            <a href={data.streamUrl} className="value-text stream-url" target="_blank" rel="noopener noreferrer">{data.streamUrl}</a>
                            <button onClick={() => copyToClipboard(data.streamUrl)} className="copy-btn-small" title="Copy stream URL">📋</button>
                        </div>
                    </div>
                </div>
                <div className="action-buttons">
                    <button onClick={() => copyToClipboard(data.streamKey)} className="primary-button">📋 Copy Stream Key</button>
                    <button onClick={handleRegisterAnother} className="secondary-button">🔄 Register or Login Another User</button>
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
};

export default SuccessPage;