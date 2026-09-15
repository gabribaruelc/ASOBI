"use client";

import { useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import styles from "../admin.module.css";
import { useAdmin } from "../../context/AdminContext";

const NAV = [
  { href: "/admin", label: "Painel" },
  { href: "/admin/produtos", label: "Produtos" },
  { href: "/admin/sobre", label: "Sobre" },
  { href: "/admin/avaliacoes", label: "Avaliações" },
  { href: "/admin/admins", label: "Admins" },
];

export default function AdminShell({ children }) {
  const { isAdmin, hydrated, currentEmail, logout } = useAdmin();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (hydrated && !isAdmin) {
      router.replace("/admin/login");
    }
  }, [hydrated, isAdmin, router]);

  if (!hydrated || !isAdmin) {
    return (
      <main className={styles.loadingScreen}>
        <p>Verificando acesso de admin...</p>
      </main>
    );
  }

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <Link href="/" className={styles.logo}>
          <span className={styles.logoAso}>aso</span>
          <span className={styles.logoBi}>bi</span>
          <span className={styles.logoTag}>admin</span>
        </Link>

        <nav className={styles.nav} aria-label="Navegação do painel admin">
          {NAV.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={`${styles.navLink} ${
                pathname === item.href ? styles.navLinkActive : ""
              }`}
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className={styles.sidebarFooter}>
          <p className={styles.currentUser}>{currentEmail}</p>
          <button
            type="button"
            className={styles.logoutButton}
            onClick={() => {
              logout();
              router.push("/admin/login");
            }}
          >
            Sair
          </button>
        </div>
      </aside>

      <main className={styles.content}>{children}</main>
    </div>
  );
}
