"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { getSupabase, isAuthEnabled } from "../lib/supabase";

const AuthContext = createContext(null);

// Conta do cliente (adulto responsável). Guardamos só o que o Google informa:
// nome e e-mail — nenhum dado de criança (LGPD).
export function AuthProvider({ children }) {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(isAuthEnabled);

  useEffect(() => {
    const supabase = getSupabase();
    if (!supabase) return;

    supabase.auth.getSession().then(({ data }) => {
      setSession(data.session);
      setLoading(false);
    });
    const { data } = supabase.auth.onAuthStateChange((_event, newSession) => {
      setSession(newSession);
      setLoading(false);
    });
    return () => data.subscription.unsubscribe();
  }, []);

  /** Leva ao Google e volta para `returnPath` já logado. */
  async function signInWithGoogle(returnPath = "/conta") {
    const supabase = getSupabase();
    if (!supabase) throw new Error("O login não está disponível no momento.");
    const { error } = await supabase.auth.signInWithOAuth({
      provider: "google",
      options: { redirectTo: `${window.location.origin}${returnPath}` },
    });
    if (error) throw new Error("Não foi possível entrar com o Google. Tente novamente.");
  }

  async function signOut() {
    await getSupabase()?.auth.signOut();
  }

  /** Token para a API do backend (renovado sozinho quando vence); null se não logado. */
  async function getAccessToken() {
    const supabase = getSupabase();
    if (!supabase) return null;
    const { data } = await supabase.auth.getSession();
    return data.session?.access_token ?? null;
  }

  const user = session?.user
    ? {
        email: session.user.email,
        name: session.user.user_metadata?.full_name || session.user.user_metadata?.name || "",
      }
    : null;

  return (
    <AuthContext.Provider
      value={{ user, loading, isAuthEnabled, signInWithGoogle, signOut, getAccessToken }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth precisa ser usado dentro de <AuthProvider>");
  }
  return context;
}
