"use client";

import styles from "../admin.module.css";
import { useProducts } from "../../context/ProductsContext";

export default function AdminAvaliacoesPage() {
  const { products, setReviewStatus, removeReview } = useProducts();

  const pending = products.flatMap((product) =>
    product.reviews
      .filter((review) => review.status === "pending")
      .map((review) => ({
        ...review,
        productSlug: product.slug,
        productName: product.name,
      }))
  );

  return (
    <div>
      <h1 className={styles.pageTitle}>Avaliações pendentes</h1>
      <p className={styles.pageDesc} style={{ marginBottom: "24px" }}>
        {pending.length} avaliação(ões) esperando aprovação.
      </p>

      {pending.length === 0 ? (
        <p className={styles.empty}>Nenhuma avaliação pendente.</p>
      ) : (
        <div className={styles.reviewsQueue}>
          {pending.map((review) => (
            <div
              key={`${review.productSlug}-${review.id}`}
              className={styles.reviewQueueCard}
            >
              <div>
                <p className={styles.reviewProduct}>{review.productName}</p>
                <p className={styles.reviewHead}>
                  <strong>{review.name}</strong> ·{" "}
                  {"★".repeat(review.rating)}
                  {"☆".repeat(5 - review.rating)}
                </p>
                <p>{review.comment}</p>
              </div>
              <div className={styles.actions}>
                <button
                  type="button"
                  className={styles.approveButton}
                  onClick={() =>
                    setReviewStatus(review.productSlug, review.id, "approved")
                  }
                >
                  Aprovar
                </button>
                <button
                  type="button"
                  className={styles.rejectButton}
                  onClick={() => removeReview(review.productSlug, review.id)}
                >
                  Rejeitar
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
