"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";
import AdminShell from "./components/AdminShell";
import { API_URL, isApiEnabled } from "../lib/api";

export default function AdminLayout({ children }) {
  const pathname = usePathname();

  // Com o backend ligado, o painel de verdade é o do Spring Boot (Thymeleaf).
  // Este painel em Next.js só vale no modo antigo (mock + localStorage).
  useEffect(() => {
    if (isApiEnabled) {
      window.location.replace(`${API_URL}/admin`);
    }
  }, []);

  if (isApiEnabled) {
    return <p style={{ padding: 24 }}>Abrindo o painel admin…</p>;
  }

  if (pathname === "/admin/login") {
    return children;
  }

  return <AdminShell>{children}</AdminShell>;
}
