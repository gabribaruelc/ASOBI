"use client";

import styles from "../../admin.module.css";
import ProdutoForm from "../../components/ProdutoForm";
import { useProducts } from "../../../context/ProductsContext";

export default function NovoProdutoPage() {
  const { addProduct } = useProducts();

  return (
    <div>
      <h1 className={styles.pageTitle}>Novo produto</h1>
      <p className={styles.pageDesc} style={{ marginBottom: "24px" }}>
        Preencha os dados do jogo. Ele aparece no catálogo assim que salvo.
      </p>
      <ProdutoForm onSubmit={addProduct} submitLabel="Adicionar produto" />
    </div>
  );
}
