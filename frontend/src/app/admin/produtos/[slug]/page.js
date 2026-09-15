"use client";

import { useParams } from "next/navigation";
import styles from "../../admin.module.css";
import ProdutoForm from "../../components/ProdutoForm";
import { useProducts } from "../../../context/ProductsContext";

export default function EditarProdutoPage() {
  const { slug } = useParams();
  const { products, updateProduct } = useProducts();
  const product = products.find((p) => p.slug === slug);

  if (!product) {
    return <p className={styles.empty}>Produto não encontrado.</p>;
  }

  function handleSubmit(updated) {
    updateProduct(product.slug, updated);
  }

  return (
    <div>
      <h1 className={styles.pageTitle}>Editar {product.name}</h1>
      <p className={styles.pageDesc} style={{ marginBottom: "24px" }}>
        As alterações valem para o catálogo público imediatamente.
      </p>
      <ProdutoForm
        initialProduct={product}
        onSubmit={handleSubmit}
        submitLabel="Salvar alterações"
      />
    </div>
  );
}
