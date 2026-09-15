"use client";

import Link from "next/link";
import styles from "./page.module.css";
import { formatPrice } from "../data/products";
import { useCart } from "../context/CartContext";
import { useProducts } from "../context/ProductsContext";

export default function CarrinhoPage() {
  const { items, updateQuantity, removeItem } = useCart();
  const { products } = useProducts();

  const cartProducts = items
    .map((item) => {
      const product = products.find((p) => p.slug === item.slug);
      return product ? { ...product, quantity: item.quantity } : null;
    })
    .filter(Boolean);

  const subtotal = cartProducts.reduce(
    (sum, product) => sum + product.price * product.quantity,
    0
  );

  return (
    <main className={styles.main}>
      <div className="container">
        <h1 className={styles.title}>Seu carrinho</h1>

        {cartProducts.length === 0 ? (
          <div className={styles.empty}>
            <span className={styles.emptyIcon} aria-hidden="true">
              🛒
            </span>
            <p>Seu carrinho está vazio.</p>
            <Link href="/jogos" className={styles.emptyLink}>
              Ver jogos
            </Link>
          </div>
        ) : (
          <div className={styles.layout}>
            <div className={styles.items}>
              {cartProducts.map((product) => (
                <div key={product.slug} className={styles.item}>
                  <div className={styles.itemImage} aria-hidden="true">
                    {product.icon}
                  </div>

                  <div className={styles.itemBody}>
                    <span className={styles.itemSkill}>{product.skill}</span>
                    <h3>{product.name}</h3>
                    <p className={styles.itemMeta}>
                      {product.age} · {product.players}
                    </p>
                  </div>

                  <div className={styles.quantity}>
                    <button
                      type="button"
                      onClick={() => updateQuantity(product.slug, -1)}
                      aria-label="Diminuir quantidade"
                    >
                      −
                    </button>
                    <span>{product.quantity}</span>
                    <button
                      type="button"
                      onClick={() => updateQuantity(product.slug, 1)}
                      aria-label="Aumentar quantidade"
                    >
                      +
                    </button>
                  </div>

                  <div className={styles.itemPrice}>
                    {formatPrice(product.price * product.quantity)}
                  </div>

                  <button
                    type="button"
                    className={styles.remove}
                    onClick={() => removeItem(product.slug)}
                    aria-label={`Remover ${product.name}`}
                  >
                    ✕
                  </button>
                </div>
              ))}

              <Link href="/jogos" className={styles.continueLink}>
                ← Continuar comprando
              </Link>
            </div>

            <aside className={styles.summary}>
              <h2>Resumo do pedido</h2>
              <div className={styles.summaryRow}>
                <span>Subtotal</span>
                <span>{formatPrice(subtotal)}</span>
              </div>
              <div className={styles.summaryRow}>
                <span>Frete</span>
                <span className={styles.summaryMuted}>
                  Calculado no checkout
                </span>
              </div>
              <div className={`${styles.summaryRow} ${styles.summaryTotal}`}>
                <span>Total</span>
                <span>{formatPrice(subtotal)}</span>
              </div>
              <button type="button" className={styles.checkoutButton}>
                Finalizar compra
              </button>
              <p className={styles.paymentNote}>
                Pagamento via Pix, cartão ou boleto
              </p>
            </aside>
          </div>
        )}
      </div>
    </main>
  );
}
