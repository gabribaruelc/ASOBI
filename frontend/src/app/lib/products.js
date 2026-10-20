// Funções pequenas sobre o produto, usadas tanto no servidor quanto no navegador.

export function formatPrice(value) {
  return value.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
}

export function isProductNew(product) {
  return Boolean(product.newUntil) && new Date(product.newUntil) > new Date();
}

// Minúsculas e sem acento, para "memoria" achar "Memória".
function normalize(text) {
  return String(text ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase();
}

/** Busca da loja: todas as palavras precisam aparecer em algum campo do produto. */
export function matchesSearch(product, search) {
  const terms = normalize(search).split(/\s+/).filter(Boolean);
  const haystack = normalize(
    [product.name, product.skill, product.age, product.players, product.description].join(" ")
  );
  return terms.every((term) => haystack.includes(term));
}

export function discountPercent(product) {
  return product.promo
    ? Math.round((1 - product.price / product.promo.originalPrice) * 100)
    : null;
}
