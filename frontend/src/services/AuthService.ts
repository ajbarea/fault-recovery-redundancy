import type { RegisterRequest } from '@/types/RegisterRequest';
import axios from 'axios';

const API_URL = '/api/auth/';

class AuthService {
    register(user: RegisterRequest) {
        return axios.post<{ streamKey: string }>(API_URL + 'register', user);
    }

    login(username: string, password: string) {
        return axios.post<{ streamKey: string; username: string; email: string; streamUrl: string }>(API_URL + 'login', { username, password });
    }
}

export default new AuthService();

