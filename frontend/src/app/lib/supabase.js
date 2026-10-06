import { createClient } from "@supabase/supabase-js";

// Login de cliente (Google) pelo Supabase Auth. As duas variáveis são públicas por
// natureza: a chave "anon"/"publishable" serve só para o navegador iniciar o login.
// Sem elas, a loja funciona normalmente, só sem a opção de entrar.
const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;

export const isAuthEnabled = Boolean(url && anonKey);

let client = null;

/** Cliente do Supabase no navegador; null se o login não está configurado. */
export function getSupabase() {
  if (!isAuthEnabled || typeof window === "undefined") return null;
  if (!client) {
    client = createClient(url, anonKey, { auth: { flowType: "pkce" } });
  }
  return client;
}
