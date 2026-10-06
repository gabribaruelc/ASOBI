"use client";

import { useState } from "react";
import Link from "next/link";
import styles from "./Header.module.css";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";

const NAV_LINKS = [
  { href: "/jogos", label: "Jogos" },
  { href: "/novidades", label: "Novidades" },
  { href: "/promocoes", label: "Promoções" },
  { href: "/sobre", label: "Sobre" },
];

const COOPERATIVE_RANGE = { href: "/jogos?estilo=cooperativos", label: "Cooperativos" };

// ageFilters ({ key, label }) vem do servidor (layout), já com as faixas do painel.
export default function Header({ ageFilters }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const { count } = useCart();
  const { user, isAuthEnabled } = useAuth();
  // Logado: "Meus pedidos". Sem login configurado, o link some (compra sem cadastro).
  const accountLink = user
    ? { href: "/conta", label: "Meus pedidos" }
    : isAuthEnabled
      ? { href: "/login", label: "Entrar" }
      : null;
  const ageRanges = [
    ...ageFilters.map((filter) => ({ href: `/jogos?idade=${filter.key}`, label: filter.label })),
    COOPERATIVE_RANGE,
  ];

  return (
    <header className={styles.header}>
      <div className={styles.topBar}>
        Frete grátis para compras acima de R$ 150 · Pix, cartão e boleto
      </div>

      <div className={`container ${styles.headerInner}`}>
        <Link href="/" className={styles.logo}>
          <span className={styles.logoAso}>aso</span>
          <span className={styles.logoBi}>bi</span>
        </Link>

        <nav className={styles.nav} aria-label="Navegação principal">
          {NAV_LINKS.map((link) => (
            <Link key={link.href} href={link.href}>
              {link.label}
            </Link>
          ))}
        </nav>

        <div className={styles.search}>
          <span aria-hidden="true">🔍</span>
          <input type="search" placeholder="Buscar jogos, idade, habilidade..." />
        </div>

        <div className={styles.actions}>
          {accountLink && (
            <Link href={accountLink.href} className={styles.loginLink}>
              {accountLink.label}
            </Link>
          )}
          <Link href="/carrinho" className={styles.iconButton} aria-label="Carrinho de compras">
            🛒
            {count > 0 && <span className={styles.badge}>{count}</span>}
          </Link>
          <button
            className={styles.menuToggle}
            aria-label="Abrir menu"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
          >
            ☰
          </button>
        </div>
      </div>

      <div className={styles.ageRail}>
        <div className={`container ${styles.ageRailInner}`}>
          {ageRanges.map((range) => (
            <Link key={range.label} href={range.href} className={styles.agePill}>
              {range.label}
            </Link>
          ))}
        </div>
      </div>

      {menuOpen && (
        <nav className="container" aria-label="Navegação móvel">
          <ul style={{ display: "flex", flexDirection: "column", gap: "4px", paddingBottom: "16px" }}>
            {NAV_LINKS.map((link) => (
              <li key={link.href}>
                <Link href={link.href} style={{ display: "block", padding: "10px 0", fontWeight: 600 }}>
                  {link.label}
                </Link>
              </li>
            ))}
            {accountLink && (
              <li>
                <Link href={accountLink.href} style={{ display: "block", padding: "10px 0", fontWeight: 600 }}>
                  {accountLink.label}
                </Link>
              </li>
            )}
          </ul>
        </nav>
      )}
    </header>
  );
}
