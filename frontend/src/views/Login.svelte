<script lang="ts">
    import { type AppState } from '../lib/api/common';

    import { getContext } from 'svelte';
    import {authenticate} from '../lib/api/auth';

    let username = $state('');
    let password = $state('');

    const app = getContext<{page: AppState }>('app');

    async function handleSubmit(event: Event) {
        event.preventDefault();

        try {
            const response = await authenticate({username, password});
            if (response.status == 200) {app.page = 'home'; console.log(response.data);}
        } catch (error) {
            console.error(error);
        }
    }
</script>

<form onsubmit={handleSubmit}>
    <input type="text" bind:value={username} placeholder="Name"/>
    <input type="text" bind:value={password} placeholder="Password"/>
    <button type="submit">Login</button>
</form>
