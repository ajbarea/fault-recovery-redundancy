import type { RegisterRequest } from '@/types/RegisterRequest';
import type { RegisterResponse } from '@/types/RegisterResponse';
import type { LoginResponse } from '@/types/LoginResponse';
import axios from 'axios';

const API_URL = 'http://localhost:8080/api/auth/';

class AuthService {
    register(user: RegisterRequest) {
        return axios.post<RegisterResponse>(API_URL + 'register', user);
    }

    login(username: string, password: string) {
        return axios.post<LoginResponse>(API_URL + 'login', { username, password });
    }
}

export default new AuthService();

