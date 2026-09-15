"use client";

import styles from "./page.module.css";
import ProductCard from "../components/ProductCard";
import { useProducts } from "../context/ProductsContext";

export default function PromocoesPage() {
  const { products } = useProducts();
  const promocoes = products.filter((product) => product.promo);

  return (
    <main>
      <section className={styles.header}>
        <div className="container">
          <p className={styles.kicker}>Preço especial</p>
          <h1>Promoções</h1>
          <p className={styles.desc}>
            Aproveite descontos por tempo limitado em jogos selecionados —
            enquanto durar o estoque.
          </p>
        </div>
      </section>

      <section className={styles.section}>
        <div className="container">
          {promocoes.length > 0 ? (
            <div className={styles.grid}>
              {promocoes.map((product) => (
                <ProductCard key={product.slug} product={product} />
              ))}
            </div>
          ) : (
            <p className={styles.empty}>
              Nenhuma promoção ativa no momento. Volte em breve!
            </p>
          )}
        </div>
      </section>
    </main>
  );
}
