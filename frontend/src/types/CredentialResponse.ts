export interface CredentialResponse {
  username: string;
  streamKey: string;
  serverUrl: string;
  streamUrl: string;
  wasLoggedIn: boolean;
  wasRegistered: boolean;
}
