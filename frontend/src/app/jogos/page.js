import Link from "next/link";
import styles from "./page.module.css";
import ProductCard from "../components/ProductCard";
import CatalogUnavailable from "../components/CatalogUnavailable";
import { getAgeFilters, getProducts } from "../lib/catalog";

export default async function JogosPage({ searchParams }) {
  const { idade, estilo } = await searchParams;
  const [products, ageFilters] = await Promise.all([
    getProducts(),
    getAgeFilters(),
  ]);

  const cooperativeOnly = estilo === "cooperativos";
  const filtered = (products || []).filter((product) => {
    if (idade && product.ageKey !== idade) return false;
    if (cooperativeOnly && !product.cooperative) return false;
    return true;
  });

  const activeLabel =
    ageFilters.find((f) => f.key === idade)?.label ||
    (cooperativeOnly ? "Cooperativos" : null);

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
              className={`${styles.pill} ${!idade && !cooperativeOnly ? styles.pillActive : ""}`}
            >
              Todos
            </Link>
            {ageFilters.map((filter) => (
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
              className={`${styles.pill} ${cooperativeOnly ? styles.pillActive : ""}`}
            >
              Cooperativos
            </Link>
          </div>

          {activeLabel && products && (
            <p className={styles.resultCount}>
              Mostrando <strong>{filtered.length}</strong> jogo(s) em{" "}
              <strong>{activeLabel}</strong>
            </p>
          )}

          {!products ? (
            <CatalogUnavailable />
          ) : filtered.length > 0 ? (
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
