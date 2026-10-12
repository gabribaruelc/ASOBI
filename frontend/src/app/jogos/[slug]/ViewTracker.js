"use client";

import { useEffect } from "react";
import { API_URL } from "../../lib/api";

// Avisa o backend que a ficha foi aberta (alimenta "mais vistos" no painel). A ficha é
// servida do cache, então a contagem precisa partir do navegador. Conta uma vez por
// produto em cada sessão do navegador e não envia nada sobre quem está vendo.
export default function ViewTracker({ slug }) {
  useEffect(() => {
    const key = `asobi:viewed:${slug}`;
    try {
      if (window.sessionStorage.getItem(key)) return;
      window.sessionStorage.setItem(key, "1");
    } catch {
      // sessionStorage indisponível (ex.: aba anônima): conta mesmo assim
    }
    fetch(`${API_URL}/api/products/${encodeURIComponent(slug)}/views`, {
      method: "POST",
      keepalive: true,
    }).catch(() => {
      // métrica não pode atrapalhar a página
    });
  }, [slug]);

  return null;
}
