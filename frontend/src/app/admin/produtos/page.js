"use client";

import Link from "next/link";
import styles from "../admin.module.css";
import {
  useProducts,
  isProductNew,
  daysRemaining,
} from "../../context/ProductsContext";
import { formatPrice } from "../../data/products";

export default function AdminProdutosPage() {
  const { products, updateProduct, removeProduct } = useProducts();

  function handleStockChange(slug, value) {
    const stock = Math.max(0, Number(value) || 0);
    updateProduct(slug, { stock });
  }

  function handleRemove(product) {
    if (window.confirm(`Remover "${product.name}" do catálogo?`)) {
      removeProduct(product.slug);
    }
  }

  return (
    <div>
      <div className={styles.pageHead}>
        <div>
          <h1 className={styles.pageTitle}>Produtos</h1>
          <p className={styles.pageDesc}>
            {products.length} jogo(s) no catálogo.
          </p>
        </div>
        <Link href="/admin/produtos/novo" className={styles.primaryButton}>
          + Novo produto
        </Link>
      </div>

      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Produto</th>
              <th>Preço</th>
              <th>Estoque</th>
              <th>Promoção</th>
              <th>Novidades</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {products.map((product) => (
              <tr key={product.slug}>
                <td>
                  <span aria-hidden="true">{product.icon}</span> {product.name}
                </td>
                <td>{formatPrice(product.price)}</td>
                <td>
                  <input
                    type="number"
                    min="0"
                    className={styles.stockInput}
                    value={product.stock}
                    onChange={(e) => handleStockChange(product.slug, e.target.value)}
                  />
                  {product.stock <= 0 && (
                    <span className={styles.badgeWarn}>Esgotado</span>
                  )}
                </td>
                <td>
                  {product.promo ? (
                    <span className={styles.badgeOn}>Ativa</span>
                  ) : (
                    <span className={styles.badgeOff}>Não</span>
                  )}
                </td>
                <td>
                  {isProductNew(product) ? (
                    <span className={styles.badgeOn}>
                      {daysRemaining(product.newUntil)} dia(s)
                    </span>
                  ) : (
                    <span className={styles.badgeOff}>—</span>
                  )}
                </td>
                <td className={styles.actions}>
                  <Link href={`/admin/produtos/${product.slug}`}>Editar</Link>
                  <button type="button" onClick={() => handleRemove(product)}>
                    Remover
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
