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

export function discountPercent(product) {
  return product.promo
    ? Math.round((1 - product.price / product.promo.originalPrice) * 100)
    : null;
}
