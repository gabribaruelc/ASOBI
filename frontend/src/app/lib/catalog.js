// Leitura do catálogo no servidor (Server Components). As respostas ficam em cache
// por 1 minuto: o que a Priscila muda no painel aparece na loja em até esse tempo,
// sem a loja bater no backend a cada visita.
import { API_URL } from "./api";

const REVALIDATE_SECONDS = 60;

async function fetchJson(path) {
  const response = await fetch(`${API_URL}${path}`, {
    headers: { Accept: "application/json" },
    next: { revalidate: REVALIDATE_SECONDS },
  });
  if (response.status === 404) return null;
  if (!response.ok) {
    throw new Error(`API respondeu ${response.status} em ${path}`);
  }
  return response.json();
}

// Backend fora do ar (ou ainda acordando no Cloud Run): devolve o valor reserva em
// vez de derrubar a página — e o build. A próxima revalidação tenta de novo.
async function fetchOr(path, fallback) {
  try {
    return (await fetchJson(path)) ?? fallback;
  } catch (error) {
    console.error(`Catálogo indisponível (${path}):`, error.message);
    return fallback;
  }
}

/** Todos os produtos, ou null se o backend não respondeu. */
export function getProducts() {
  return fetchOr("/api/products", null);
}

/** Um produto pelo slug; null se não existe. Lança erro se o backend não respondeu. */
export function getProduct(slug) {
  return fetchJson(`/api/products/${encodeURIComponent(slug)}`);
}

/** Faixas etárias ({ key, label }) na ordem definida no painel. */
export async function getAgeFilters() {
  const categories = await fetchOr("/api/categories", []);
  return categories.map((category) => ({
    key: category.slug,
    label: category.name,
  }));
}

/** Conteúdo da página Sobre; null se o backend não respondeu. */
export function getAbout() {
  return fetchOr("/api/content/about", null);
}
