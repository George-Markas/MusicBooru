export const BASE_URL = 'http://localhost:8080/api';

export type AppState = 'login' | 'home' | 'playlists' | 'error' | 'loading';
export type Result<T> = {ok: true, status: number, data: T} | {ok: false, status: number};
 
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

    if (!response.ok) {
        return {ok: false, status: response.status};
    }

    return {ok: true, status: response.status, data: await response.json()};
}