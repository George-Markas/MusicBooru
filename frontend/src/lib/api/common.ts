export const BASE_URL = 'http://localhost:8080/api';

export type AppState = 'login' | 'home' | 'playlists' | 'error' | 'loading';
export type Result<T> = {status: number, data: T}
 
export async function api<T>(url: string, options: RequestInit = {}) : Promise<Result<T>> {
    const response = await fetch(`${BASE_URL}/${url}`, {
            credentials: 'include',
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            }
        }
    );

    return {status: response.status, data: await response.json()};
}