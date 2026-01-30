import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1';

const API_KEY_STORAGE_KEY = 'nimetro_api_key';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor to add API key
    this.client.interceptors.request.use(
      (config) => {
        const apiKey = this.getApiKey();
        if (apiKey) {
          config.headers['X-API-Key'] = apiKey;
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401 || error.response?.status === 403) {
          // API key is invalid or missing
          this.clearApiKey();
        }
        return Promise.reject(error);
      }
    );
  }

  getApiKey(): string | null {
    return localStorage.getItem(API_KEY_STORAGE_KEY);
  }

  setApiKey(apiKey: string): void {
    localStorage.setItem(API_KEY_STORAGE_KEY, apiKey);
  }

  clearApiKey(): void {
    localStorage.removeItem(API_KEY_STORAGE_KEY);
  }

  hasApiKey(): boolean {
    return this.getApiKey() !== null;
  }

  getClient(): AxiosInstance {
    return this.client;
  }
}

export const apiClient = new ApiClient();

