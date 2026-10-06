"use client";

// Erro inesperado ao montar uma página (ex.: backend fora do ar ao abrir um jogo).
export default function ErrorPage({ reset }) {
  return (
    <main style={{ padding: "64px 0", textAlign: "center" }}>
      <div className="container">
        <h1>Ops, algo não saiu como esperado 🎲</h1>
        <p style={{ margin: "12px 0 24px" }}>
          Não conseguimos carregar esta página agora. Tente de novo em instantes.
        </p>
        <button type="button" onClick={() => reset()}>
          Tentar de novo
        </button>
      </div>
    </main>
  );
}
