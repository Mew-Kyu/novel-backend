// Custom API wrapper for easier usage
import { Configuration, DefaultApi } from './generated';

export class NovelApiClient {
  private api: DefaultApi;
  private token: string | null = null;

  constructor(basePath: string = 'http://localhost:8080') {
    const config = new Configuration({
      basePath,
      accessToken: () => this.token || '',
    });
    this.api = new DefaultApi(config);
  }

  // Authentication
  setToken(token: string) {
    this.token = token;
    if (typeof window !== 'undefined') {
      localStorage.setItem('accessToken', token);
    }
  }

  clearToken() {
    this.token = null;
    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken');
    }
  }

  getToken(): string | null {
    if (!this.token && typeof window !== 'undefined') {
      this.token = localStorage.getItem('accessToken');
    }
    return this.token;
  }

  // Re-export API instance for direct access
  get raw() {
    return this.api;
  }
}

// Create singleton instance
const apiClient = new NovelApiClient();

// Auto-load token from localStorage
if (typeof window !== 'undefined') {
  const savedToken = localStorage.getItem('accessToken');
  if (savedToken) {
    apiClient.setToken(savedToken);
  }
}

export default apiClient;

// Re-export all types and models
export * from './generated';
