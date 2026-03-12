
import { type Result, api} from "./common";

interface AuthRequest {
    username: string;
    password: string;
}

const METHOD = 'POST';

export async function authenticate(credentials: AuthRequest) : Promise<Result<string>> {
    const path = 'auth/authenticate';
    const response = await api<string>(path, {
            method: METHOD, 
            body: JSON.stringify(credentials)
        })

    return response;
}

export async function logout() : Promise<Result<string>> {
    const path = 'auth/logout'
    const response = await api<string>(path, {
        method: METHOD
    })

    return response;
}