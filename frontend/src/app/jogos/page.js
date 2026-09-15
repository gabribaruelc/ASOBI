"use client";

import { Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import styles from "./page.module.css";
import ProductCard from "../components/ProductCard";
import { AGE_FILTERS } from "../data/products";
import { useProducts } from "../context/ProductsContext";

export default function JogosPage() {
  return (
    <Suspense fallback={null}>
      <JogosContent />
    </Suspense>
  );
}

function JogosContent() {
  const searchParams = useSearchParams();
  const idade = searchParams.get("idade");
  const estilo = searchParams.get("estilo");
  const { products } = useProducts();

  const filtered = products.filter((product) => {
    if (idade && product.ageKey !== idade) return false;
    if (estilo && product.estilo !== estilo) return false;
    return true;
  });

  const activeLabel =
    AGE_FILTERS.find((f) => f.key === idade)?.label ||
    (estilo === "cooperativos" ? "Cooperativos" : null);

  return (
    <main>
      <section className={styles.header}>
        <div className="container">
          <p className={styles.kicker}>Catálogo</p>
          <h1>Todos os jogos</h1>
          <p className={styles.desc}>
            Cada jogo mostra a idade recomendada, o número de jogadores e a
            habilidade que estimula, para você escolher com confiança.
          </p>
        </div>
      </section>

      <section className={styles.section}>
        <div className="container">
          <div className={styles.filters}>
            <Link
              href="/jogos"
              className={`${styles.pill} ${!idade && !estilo ? styles.pillActive : ""}`}
            >
              Todos
            </Link>
            {AGE_FILTERS.map((filter) => (
              <Link
                key={filter.key}
                href={`/jogos?idade=${filter.key}`}
                className={`${styles.pill} ${idade === filter.key ? styles.pillActive : ""}`}
              >
                {filter.label}
              </Link>
            ))}
            <Link
              href="/jogos?estilo=cooperativos"
              className={`${styles.pill} ${estilo === "cooperativos" ? styles.pillActive : ""}`}
            >
              Cooperativos
            </Link>
          </div>

          {activeLabel && (
            <p className={styles.resultCount}>
              Mostrando <strong>{filtered.length}</strong> jogo(s) em{" "}
              <strong>{activeLabel}</strong>
            </p>
          )}

          {filtered.length > 0 ? (
            <div className={styles.grid}>
              {filtered.map((product) => (
                <ProductCard key={product.slug} product={product} />
              ))}
            </div>
          ) : (
            <div className={styles.empty}>
              <p>Nenhum jogo encontrado com esse filtro.</p>
              <Link href="/jogos" className={styles.clearLink}>
                Ver todos os jogos
              </Link>
            </div>
          )}
        </div>
      </section>
    </main>
  );
}
