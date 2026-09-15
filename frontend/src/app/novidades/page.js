"use client";

import styles from "./page.module.css";
import ProductCard from "../components/ProductCard";
import { useProducts, isProductNew } from "../context/ProductsContext";

export default function NovidadesPage() {
  const { products } = useProducts();
  const novidades = products.filter(isProductNew);

  return (
    <main>
      <section className={styles.header}>
        <div className="container">
          <p className={styles.kicker}>Recém-chegados</p>
          <h1>Novidades</h1>
          <p className={styles.desc}>
            Fique por dentro dos jogos mais novos da nossa curadoria — sempre
            escolhidos pensando na habilidade que estimulam.
          </p>
        </div>
      </section>

      <section className={styles.section}>
        <div className="container">
          {novidades.length > 0 ? (
            <div className={styles.grid}>
              {novidades.map((product) => (
                <ProductCard key={product.slug} product={product} />
              ))}
            </div>
          ) : (
            <p className={styles.empty}>
              Nenhuma novidade no momento. Volte em breve!
            </p>
          )}
        </div>
      </section>
    </main>
  );
}
