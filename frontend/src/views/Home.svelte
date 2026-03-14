<script lang="ts">
    import { getContext } from "svelte";
    import { type AppState } from "../lib/api/common";
    import {logout} from "../lib/api/auth"

    const app = getContext<{page: AppState }>('app');

    async function handleLogout() {
        try {
            const response = await logout();
            if (response.ok) {app.page = 'login'; console.log(response.data);}
        } catch (error) {
            console.error(error);
        }
    }

</script>

<p>88 == Welcome to musicbooru == 88</p>
<button onclick={handleLogout}>Logout</button>