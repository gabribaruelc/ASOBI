import Link from "next/link";
import styles from "./page.module.css";

export default function ProdutoNotFound() {
  return (
    <main className={styles.main}>
      <div className="container">
        <p>Jogo não encontrado.</p>
        <Link href="/jogos">Voltar para o catálogo</Link>
      </div>
    </main>
  );
}
