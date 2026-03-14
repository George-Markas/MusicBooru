import { api, BASE_URL } from "./common"

export async function get_tracks(): Promise<any> {
    const path = `track`

    const response = await api(path, {
        method: 'GET'
    });

    return response;
} 