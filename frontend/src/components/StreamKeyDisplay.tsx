import React, { useState } from 'react';

interface StreamKeyDisplayProps {
  streamKey: string;
  username: string;
  rtmpUrl?: string;
}

const StreamKeyDisplay: React.FC<StreamKeyDisplayProps> = ({
  streamKey,
  rtmpUrl = 'rtmp://localhost:1935/live'
}) => {
  const [copyFeedback, setCopyFeedback] = useState<string>('');

  const copyToClipboard = async (text: string, label: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopyFeedback(`${label} copied!`);
      setTimeout(() => setCopyFeedback(''), 2000);
    } catch (err) {
      setCopyFeedback('Failed to copy');
      setTimeout(() => setCopyFeedback(''), 2000);
    }
  };

  return (
    <div className="stream-key-display">
      <div className="stream-key-header">
        <h2>Your Streaming Credentials</h2>
        <p>Use these credentials to set up your streaming software</p>
      </div>

      {copyFeedback && (
        <div className="copy-feedback">
          {copyFeedback}
        </div>
      )}

      <div className="credentials-grid">
        <div className="credential-row">
          <label className="credential-label">RTMP Server URL</label>
          <div className="credential-value-container">
            <code className="credential-value">{rtmpUrl}</code>
            <button
              className="copy-button"
              onClick={() => copyToClipboard(rtmpUrl, 'RTMP URL')}
              title="Copy RTMP URL"
            >
              📋
            </button>
          </div>
        </div>

        <div className="credential-row featured">
          <label className="credential-label">Stream Key</label>
          <div className="credential-value-container">
            <code className="credential-value stream-key">{streamKey}</code>
            <button
              className="copy-button primary"
              onClick={() => copyToClipboard(streamKey, 'Stream key')}
              title="Copy stream key"
            >
              📋
            </button>
          </div>
        </div>
      </div>

      <div className="obs-instructions">
        <h3>OBS Studio Setup Instructions</h3>
        <div className="instruction-steps">
          <div className="step">
            <span className="step-number">1</span>
            <div className="step-content">
              <strong>Open OBS Studio</strong>
              <p>Launch OBS Studio on your computer</p>
            </div>
          </div>

          <div className="step">
            <span className="step-number">2</span>
            <div className="step-content">
              <strong>Go to Settings → Stream</strong>
              <p>Click on Settings in the bottom right, then select the Stream tab</p>
            </div>
          </div>

          <div className="step">
            <span className="step-number">3</span>
            <div className="step-content">
              <strong>Configure Stream Settings</strong>
              <ul>
                <li>Service: Custom</li>
                <li>Server: <code>{rtmpUrl}</code></li>
                <li>Stream Key: <code className="inline-stream-key">{streamKey}</code></li>
              </ul>
            </div>
          </div>

          <div className="step">
            <span className="step-number">4</span>
            <div className="step-content">
              <strong>Start Streaming</strong>
              <p>Click "Start Streaming" in OBS to begin broadcasting</p>
            </div>
          </div>
        </div>
      </div>

      <div className="quick-copy-section">
        <button
          className="quick-copy-button"
          onClick={() => copyToClipboard(streamKey, 'Stream key')}
        >
          📋 Copy Stream Key
        </button>
      </div>
    </div>
  );
};

export default StreamKeyDisplay;