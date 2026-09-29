import { defineCloudflareConfig } from "@opennextjs/cloudflare";

// Sem cache incremental (R2): a loja não usa ISR e assim fica sem custo extra.
export default defineCloudflareConfig();
