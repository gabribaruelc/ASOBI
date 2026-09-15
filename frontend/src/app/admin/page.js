"use client";

import Link from "next/link";
import styles from "./admin.module.css";
import { useProducts, isProductNew } from "../context/ProductsContext";

export default function AdminDashboardPage() {
  const { products } = useProducts();

  const outOfStock = products.filter((product) => product.stock <= 0).length;
  const inPromo = products.filter((product) => product.promo).length;
  const inNovidades = products.filter(isProductNew).length;
  const pendingReviews = products.reduce(
    (sum, product) =>
      sum + product.reviews.filter((review) => review.status === "pending").length,
    0
  );

  const cards = [
    { label: "Produtos no catálogo", value: products.length, href: "/admin/produtos" },
    { label: "Sem estoque", value: outOfStock, href: "/admin/produtos" },
    { label: "Em promoção", value: inPromo, href: "/admin/produtos" },
    { label: "Em Novidades", value: inNovidades, href: "/admin/produtos" },
    { label: "Avaliações pendentes", value: pendingReviews, href: "/admin/avaliacoes" },
  ];

  return (
    <div>
      <h1 className={styles.pageTitle}>Painel</h1>
      <p className={styles.pageDesc}>Visão geral da loja ASOBI.</p>

      <div className={styles.statsGrid} style={{ marginTop: "24px" }}>
        {cards.map((card) => (
          <Link key={card.label} href={card.href} className={styles.statCard}>
            <span className={styles.statValue}>{card.value}</span>
            <span className={styles.statLabel}>{card.label}</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
