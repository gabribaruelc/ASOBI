// Mostrado no lugar da lista de jogos quando o backend não respondeu.
export default function CatalogUnavailable() {
  return (
    <p role="status" style={{ padding: "32px 0", textAlign: "center" }}>
      Não conseguimos carregar os jogos agora. Atualize a página em instantes. 🎲
    </p>
  );
}
