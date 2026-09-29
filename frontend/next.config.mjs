import { initOpenNextCloudflareForDev } from "@opennextjs/cloudflare";

/** @type {import('next').NextConfig} */
const nextConfig = {
  /* config options here */
};

export default nextConfig;

// Deixa o `next dev` enxergar os bindings do Cloudflare (wrangler.jsonc).
initOpenNextCloudflareForDev();
