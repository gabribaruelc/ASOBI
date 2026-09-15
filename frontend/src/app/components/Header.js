"use client";

import { useState } from "react";
import Link from "next/link";
import styles from "./Header.module.css";
import { useCart } from "../context/CartContext";

const NAV_LINKS = [
  { href: "/jogos", label: "Jogos" },
  { href: "/novidades", label: "Novidades" },
  { href: "/promocoes", label: "Promoções" },
  { href: "/sobre", label: "Sobre" },
];

const AGE_RANGES = [
  { href: "/jogos?idade=ate-3", label: "Até 3 anos" },
  { href: "/jogos?idade=4-6", label: "4 a 6 anos" },
  { href: "/jogos?idade=7-10", label: "7 a 10 anos" },
  { href: "/jogos?idade=mais-10", label: "+10 anos" },
  { href: "/jogos?estilo=cooperativos", label: "Cooperativos" },
];

export default function Header() {
  const [menuOpen, setMenuOpen] = useState(false);
  const { count } = useCart();

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
          <Link href="/login" className={styles.loginLink}>
            Entrar
          </Link>
          <Link href="/cadastro" className={styles.signupLink}>
            Cadastrar
          </Link>
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
          {AGE_RANGES.map((range) => (
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
            <li>
              <Link href="/cadastro" style={{ display: "block", padding: "10px 0", fontWeight: 600 }}>
                Cadastrar
              </Link>
            </li>
          </ul>
        </nav>
      )}
    </header>
  );
}
