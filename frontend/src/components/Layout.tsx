import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import ServiceStatus from './ServiceStatus';

interface LayoutProps {
    children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
    const location = useLocation();

    return (
        <div className="app-layout">
            <header className="app-header">
                <div className="header-content">
                    <h1 className="app-title">
                        <Link to="/" className="title-link">
                            🎥 Stream Setup
                        </Link>
                    </h1>
                    <nav className="app-nav">
                        {location.pathname === '/success' && (
                            <Link to="/" className="nav-link">
                                ← Back to Registration
                            </Link>
                        )}
                    </nav>
                </div>
            </header>
            <main className="app-main">
                <ServiceStatus />
                {children}
            </main>
        </div>
    );
};

export default Layout;