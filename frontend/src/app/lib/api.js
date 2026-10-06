// Cliente da API do backend (Spring Boot, em ../backend). O catálogo, os pedidos e o
// conteúdo da loja vêm todos de lá; em desenvolvimento, suba o backend na porta 8080.
export const API_URL = (
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
).replace(/\/$/, "");

export async function apiFetch(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    let detail = "Não foi possível falar com a loja. Tente novamente.";
    let fieldErrors = {};
    try {
      const problem = await response.json();
      if (problem.detail) detail = problem.detail;
      if (problem.errors) fieldErrors = problem.errors;
    } catch {
      // resposta sem corpo JSON — mantém a mensagem genérica
    }
    const error = new Error(detail);
    error.status = response.status;
    // Erros por campo (ex.: { "customer.email": "E-mail inválido." })
    error.fieldErrors = fieldErrors;
    throw error;
  }

  return response.status === 204 ? null : response.json();
}
