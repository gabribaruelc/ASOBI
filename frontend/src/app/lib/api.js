// Cliente da API do backend (Spring Boot, em ../backend).
// Sem NEXT_PUBLIC_API_URL definido, a loja continua no modo antigo
// (dados mockados + localStorage) — útil enquanto a migração não é confirmada.
export const API_URL = process.env.NEXT_PUBLIC_API_URL?.replace(/\/$/, "") || null;

export const isApiEnabled = Boolean(API_URL);

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

// Converte o produto da API para o formato que as telas já usam
// (o mesmo de INITIAL_PRODUCTS), para não precisar mexer em cada página.
export function toStoreProduct(apiProduct) {
  return {
    ...apiProduct,
    estilo: apiProduct.cooperative ? "cooperativos" : null,
    // A API não expõe a quantidade em estoque, só se há estoque.
    stock: apiProduct.inStock ? 1 : 0,
    reviews: apiProduct.reviews.map((review) => ({
      ...review,
      id: String(review.id),
      status: "approved",
    })),
  };
}
