export interface RegisterResponse {
    message: string;
    username: string;
    email: string;
    streamKey: string;
    serverUrl: string;
    streamUrl: string;
    wasRegistered: boolean;
    wasLoggedIn: boolean;
}
