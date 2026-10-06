"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "./api";

// Catálogo buscado no navegador, para o carrinho e o checkout: ali o preço e o
// estoque precisam ser os de agora, não os do cache das páginas.
export function useProducts() {
  const [state, setState] = useState({ products: [], loading: true, error: null });

  useEffect(() => {
    let active = true;
    apiFetch("/api/products")
      .then((products) => {
        if (active) setState({ products, loading: false, error: null });
      })
      .catch((error) => {
        if (active) setState({ products: [], loading: false, error: error.message });
      });
    return () => {
      active = false;
    };
  }, []);

  return state;
}
