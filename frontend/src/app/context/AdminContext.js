"use client";

import { createContext, useContext, useEffect, useState } from "react";

const AdminContext = createContext(null);
const ADMINS_KEY = "asobi-admins";
const SESSION_KEY = "asobi-admin-session";

// Lista inicial de e-mails com acesso ao painel. Primeiro acesso: entre com
// esse e-mail e depois cadastre o e-mail real da Priscila em /admin/admins.
const DEFAULT_ADMINS = ["priscila@asobi.com.br"];

export function AdminProvider({ children }) {
  const [admins, setAdmins] = useState(DEFAULT_ADMINS);
  const [currentEmail, setCurrentEmail] = useState(null);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    try {
      const storedAdmins = window.localStorage.getItem(ADMINS_KEY);
      // eslint-disable-next-line react-hooks/set-state-in-effect -- localStorage só existe no client; lido após a hidratação para não gerar mismatch com o SSR
      if (storedAdmins) setAdmins(JSON.parse(storedAdmins));
      const storedSession = window.localStorage.getItem(SESSION_KEY);
      if (storedSession) setCurrentEmail(storedSession);
    } catch {
      // localStorage indisponível — segue sem sessão de admin
    }
    setHydrated(true);
  }, []);

  useEffect(() => {
    if (!hydrated) return;
    try {
      window.localStorage.setItem(ADMINS_KEY, JSON.stringify(admins));
    } catch {
      // ignora falha ao persistir
    }
  }, [admins, hydrated]);

  function login(email) {
    const normalized = email.trim().toLowerCase();
    if (!admins.includes(normalized)) {
      return false;
    }
    setCurrentEmail(normalized);
    try {
      window.localStorage.setItem(SESSION_KEY, normalized);
    } catch {
      // ignora falha ao persistir sessão
    }
    return true;
  }

  function logout() {
    setCurrentEmail(null);
    try {
      window.localStorage.removeItem(SESSION_KEY);
    } catch {
      // ignora falha ao limpar sessão
    }
  }

  function addAdmin(email) {
    const normalized = email.trim().toLowerCase();
    if (!normalized || admins.includes(normalized)) return;
    setAdmins((prev) => [...prev, normalized]);
  }

  function removeAdmin(email) {
    setAdmins((prev) => prev.filter((admin) => admin !== email));
    if (currentEmail === email) logout();
  }

  const isAdmin = hydrated && currentEmail !== null && admins.includes(currentEmail);

  return (
    <AdminContext.Provider
      value={{
        admins,
        currentEmail,
        isAdmin,
        hydrated,
        login,
        logout,
        addAdmin,
        removeAdmin,
      }}
    >
      {children}
    </AdminContext.Provider>
  );
}

export function useAdmin() {
  const context = useContext(AdminContext);
  if (!context) {
    throw new Error("useAdmin precisa ser usado dentro de <AdminProvider>");
  }
  return context;
}
