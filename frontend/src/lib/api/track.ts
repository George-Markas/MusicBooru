import { BASE_URL } from "./common"

export async function get_tracks(): Promise<any> {
    const path = `${BASE_URL}/track`

    const response = await fetch(path, {
        method: 'GET',
        headers: {'Content-Type': 'application/json'},
        credentials: 'include',
    });

    return response;
} 