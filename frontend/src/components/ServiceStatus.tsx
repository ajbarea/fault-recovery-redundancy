import React, { useState, useEffect } from 'react';

const ServiceStatus: React.FC = () => {
    const [isOnline, setIsOnline] = useState<boolean | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const checkServiceStatus = async () => {
        try {
            const response = await fetch('/heartbeat/status');
            setIsOnline(response.ok);
        } catch (error) {
            console.error('Failed to check service status:', error);
            setIsOnline(false);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        checkServiceStatus();

        // Check status every 30 seconds
        const interval = setInterval(checkServiceStatus, 30000);

        return () => clearInterval(interval);
    }, []);

    if (isLoading) {
        return (
            <div className="service-status">
                <div className="status-indicator loading">
                    <span className="status-dot"></span>
                    <span className="status-text">Checking Service...</span>
                </div>
            </div>
        );
    }

    return (
        <div className="service-status">
            <div className={`status-indicator ${isOnline ? 'online' : 'offline'}`}>
                <span className="status-dot"></span>
                <span className="status-text">
                    {isOnline ? 'Service Online' : 'Service Offline'}
                </span>
            </div>
        </div>
    );
};

export default ServiceStatus;