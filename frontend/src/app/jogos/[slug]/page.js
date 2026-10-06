"use client";

import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import styles from "./page.module.css";
import ProductCard from "../../components/ProductCard";
import AddToCartBox from "../../components/AddToCartBox";
import ProductGallery from "../../components/ProductGallery";
import { formatPrice } from "../../data/products";
import { useProducts, isProductNew } from "../../context/ProductsContext";

export default function ProdutoPage() {
  const { slug } = useParams();
  const { products, addReview, loading } = useProducts();
  const product = products.find((p) => p.slug === slug);

  const [reviewName, setReviewName] = useState("");
  const [reviewRating, setReviewRating] = useState("5");
  const [reviewComment, setReviewComment] = useState("");
  const [reviewSent, setReviewSent] = useState(false);
  const [reviewSending, setReviewSending] = useState(false);
  const [reviewError, setReviewError] = useState(null);

  if (!product && loading) {
    return (
      <main className={styles.main}>
        <div className="container">
          <p>Carregando…</p>
        </div>
      </main>
    );
  }

  if (!product) {
    return (
      <main className={styles.main}>
        <div className="container">
          <p>Jogo não encontrado.</p>
          <Link href="/jogos">Voltar para o catálogo</Link>
        </div>
      </main>
    );
  }

  const isNew = isProductNew(product);
  const discount = product.promo
    ? Math.round((1 - product.price / product.promo.originalPrice) * 100)
    : null;

  const related = products
    .filter((p) => p.ageKey === product.ageKey && p.slug !== product.slug)
    .slice(0, 4);

  const approvedReviews = (product.reviews || []).filter(
    (review) => review.status === "approved"
  );
  const averageRating = approvedReviews.length
    ? approvedReviews.reduce((sum, r) => sum + r.rating, 0) /
      approvedReviews.length
    : null;

  async function handleReviewSubmit(event) {
    event.preventDefault();
    setReviewSending(true);
    setReviewError(null);
    try {
      await addReview(product.slug, {
        id: crypto.randomUUID(),
        name: reviewName,
        rating: Number(reviewRating),
        comment: reviewComment,
        status: "pending",
      });
    } catch (error) {
      setReviewError(error.message);
      return;
    } finally {
      setReviewSending(false);
    }
    setReviewName("");
    setReviewRating("5");
    setReviewComment("");
    setReviewSent(true);
  }

  return (
    <main className={styles.main}>
      <div className="container">
        <nav className={styles.breadcrumb} aria-label="Breadcrumb">
          <Link href="/">Início</Link>
          <span aria-hidden="true">/</span>
          <Link href="/jogos">Jogos</Link>
          <span aria-hidden="true">/</span>
          <span className={styles.breadcrumbCurrent}>{product.name}</span>
        </nav>

        <div className={styles.productLayout}>
          {/* key: ao trocar de produto, a galeria volta para a primeira foto */}
          <ProductGallery
            key={product.slug}
            images={product.images}
            icon={product.icon}
            name={product.name}
          >
            {isNew && <span className={styles.badgeNew}>Novo</span>}
            {discount !== null && (
              <span className={styles.badgePromo}>-{discount}%</span>
            )}
          </ProductGallery>

          <div className={styles.info}>
            <span className={styles.skill}>{product.skill}</span>
            <h1>{product.name}</h1>

            {averageRating !== null ? (
              <p className={styles.rating}>
                <span aria-hidden="true">
                  {"★".repeat(Math.round(averageRating))}
                  {"☆".repeat(5 - Math.round(averageRating))}
                </span>
                {averageRating.toFixed(1)} · {approvedReviews.length}{" "}
                avaliação(ões)
              </p>
            ) : (
              <p className={styles.ratingEmpty}>Ainda sem avaliações</p>
            )}

            <dl className={styles.specs}>
              <div>
                <dt>Idade recomendada</dt>
                <dd>{product.age}</dd>
              </div>
              <div>
                <dt>Jogadores</dt>
                <dd>{product.players}</dd>
              </div>
              <div>
                <dt>Habilidade estimulada</dt>
                <dd>{product.skill}</dd>
              </div>
            </dl>

            <div className={styles.priceBlock}>
              {product.promo && (
                <span className={styles.oldPrice}>
                  {formatPrice(product.promo.originalPrice)}
                </span>
              )}
              <span className={styles.price}>
                {formatPrice(product.price)}
              </span>
            </div>

            <p className={styles.description}>{product.description}</p>

            <AddToCartBox slug={product.slug} outOfStock={product.stock <= 0} />

            <div className={styles.paymentBadges}>
              <span>Pix</span>
              <span>Cartão de crédito</span>
              <span>Boleto</span>
            </div>
          </div>
        </div>

        <section className={styles.reviewsSection}>
          <h2>Avaliações de quem já comprou</h2>

          {approvedReviews.length > 0 ? (
            <div className={styles.reviewsList}>
              {approvedReviews.map((review) => (
                <div key={review.id} className={styles.reviewCard}>
                  <div className={styles.reviewHead}>
                    <span className={styles.reviewName}>{review.name}</span>
                    <span aria-hidden="true">
                      {"★".repeat(review.rating)}
                      {"☆".repeat(5 - review.rating)}
                    </span>
                  </div>
                  <p>{review.comment}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className={styles.reviewsEmpty}>
              Este jogo ainda não tem avaliações aprovadas. Seja o primeiro a
              avaliar!
            </p>
          )}

          {reviewSent ? (
            <p className={styles.reviewNote}>
              Recebemos sua avaliação! Ela aparece aqui depois que a nossa
              equipe aprovar.
            </p>
          ) : (
            <form className={styles.reviewForm} onSubmit={handleReviewSubmit}>
              <h3>Deixe sua avaliação</h3>
              <div className={styles.reviewFormRow}>
                <label className={styles.field}>
                  <span>Seu nome</span>
                  <input
                    type="text"
                    name="name"
                    placeholder="Seu nome"
                    value={reviewName}
                    onChange={(e) => setReviewName(e.target.value)}
                    maxLength={80}
                    required
                  />
                </label>
                <label className={styles.field}>
                  <span>Nota</span>
                  <select
                    name="rating"
                    value={reviewRating}
                    onChange={(e) => setReviewRating(e.target.value)}
                  >
                    <option value="5">★★★★★</option>
                    <option value="4">★★★★☆</option>
                    <option value="3">★★★☆☆</option>
                    <option value="2">★★☆☆☆</option>
                    <option value="1">★☆☆☆☆</option>
                  </select>
                </label>
              </div>
              <label className={styles.field}>
                <span>Comentário</span>
                <textarea
                  name="comment"
                  rows={3}
                  placeholder="Conte como foi a experiência com esse jogo"
                  value={reviewComment}
                  onChange={(e) => setReviewComment(e.target.value)}
                  maxLength={1000}
                  required
                />
              </label>
              {reviewError && (
                <p className={styles.reviewNote} role="alert">
                  {reviewError}
                </p>
              )}
              <button
                type="submit"
                className={styles.reviewSubmit}
                disabled={reviewSending}
              >
                {reviewSending ? "Enviando…" : "Enviar avaliação"}
              </button>
              <p className={styles.reviewNote}>
                Sua avaliação passa por aprovação da nossa equipe antes de
                aparecer na página.
              </p>
            </form>
          )}
        </section>

        {related.length > 0 && (
          <section className={styles.relatedSection}>
            <h2>Você também pode gostar</h2>
            <div className={styles.relatedGrid}>
              {related.map((item) => (
                <ProductCard key={item.slug} product={item} />
              ))}
            </div>
          </section>
        )}
      </div>
    </main>
  );
}
