import Link from "next/link";
import { notFound } from "next/navigation";
import styles from "./page.module.css";
import ProductCard from "../../components/ProductCard";
import AddToCartBox from "../../components/AddToCartBox";
import ProductGallery from "../../components/ProductGallery";
import ReviewForm from "./ReviewForm";
import { getProduct, getProducts } from "../../lib/catalog";
import { discountPercent, formatPrice, isProductNew } from "../../lib/products";

// Gera as fichas dos produtos já cadastrados no build; produto novo é gerado na
// primeira visita. Depois disso, cada ficha se atualiza sozinha (ver lib/catalog.js).
export async function generateStaticParams() {
  const products = await getProducts();
  return (products || []).map((product) => ({ slug: product.slug }));
}

export async function generateMetadata({ params }) {
  const { slug } = await params;
  const product = await getProduct(slug);
  if (!product) {
    return { title: "Jogo não encontrado | ASOBI" };
  }
  const description = `${product.name}: ${product.age}, ${product.players}. Estimula ${product.skill.toLowerCase()}. ${product.description}`.slice(0, 160);
  return {
    title: `${product.name} | ASOBI`,
    description,
    openGraph: {
      title: product.name,
      description,
      images: product.images.slice(0, 1),
    },
  };
}

export default async function ProdutoPage({ params }) {
  const { slug } = await params;
  const [product, products] = await Promise.all([getProduct(slug), getProducts()]);
  if (!product) {
    notFound();
  }

  const isNew = isProductNew(product);
  const discount = discountPercent(product);
  const related = (products || [])
    .filter((p) => p.ageKey === product.ageKey && p.slug !== product.slug)
    .slice(0, 4);

  // A API só devolve avaliações já aprovadas no painel.
  const reviews = product.reviews;
  const averageRating = reviews.length
    ? reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length
    : null;

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
                {averageRating.toFixed(1)} · {reviews.length} avaliação(ões)
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
              <span className={styles.price}>{formatPrice(product.price)}</span>
            </div>

            <p className={styles.description}>{product.description}</p>

            <AddToCartBox slug={product.slug} outOfStock={!product.inStock} />

            <div className={styles.paymentBadges}>
              <span>Pix</span>
              <span>Cartão de crédito</span>
              <span>Boleto</span>
            </div>
          </div>
        </div>

        <section className={styles.reviewsSection}>
          <h2>Avaliações de quem já comprou</h2>

          {reviews.length > 0 ? (
            <div className={styles.reviewsList}>
              {reviews.map((review) => (
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

          <ReviewForm slug={product.slug} />
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
