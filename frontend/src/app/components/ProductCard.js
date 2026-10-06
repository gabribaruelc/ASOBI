import Link from "next/link";
import styles from "./ProductCard.module.css";
import { formatPrice } from "../data/products";
import { isProductNew } from "../context/ProductsContext";

export default function ProductCard({ product }) {
  const { slug, icon, images, skill, name, age, players, price, promo, stock } =
    product;
  const isNew = isProductNew(product);
  const outOfStock = stock <= 0;
  // Sem foto cadastrada, mostra o emoji do produto.
  const cover = images?.[0];

  return (
    <Link href={`/jogos/${slug}`} className={styles.card}>
      <div className={styles.image} aria-hidden="true">
        {cover ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={cover} alt="" className={styles.photo} loading="lazy" />
        ) : (
          icon
        )}
        {isNew && <span className={styles.badgeNew}>Novo</span>}
        {promo && (
          <span className={styles.badgePromo}>
            -{Math.round((1 - price / promo.originalPrice) * 100)}%
          </span>
        )}
        {outOfStock && <span className={styles.badgeOut}>Esgotado</span>}
      </div>
      <div className={styles.body}>
        <span className={styles.skill}>{skill}</span>
        <h3>{name}</h3>
        <p className={styles.meta}>
          {age} · {players}
        </p>
        <div className={styles.footer}>
          <div className={styles.prices}>
            {promo && (
              <span className={styles.oldPrice}>
                {formatPrice(promo.originalPrice)}
              </span>
            )}
            <span className={styles.price}>{formatPrice(price)}</span>
          </div>
          <span className={styles.button}>Ver mais</span>
        </div>
      </div>
    </Link>
  );
}
