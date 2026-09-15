"use client";

import { createContext, useContext, useEffect, useState } from "react";

const CartContext = createContext(null);
const STORAGE_KEY = "asobi-cart";

export function CartProvider({ children }) {
  const [items, setItems] = useState([]);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    try {
      const stored = window.localStorage.getItem(STORAGE_KEY);
      // eslint-disable-next-line react-hooks/set-state-in-effect -- localStorage só existe no client; lido após a hidratação para não gerar mismatch com o SSR
      if (stored) setItems(JSON.parse(stored));
    } catch {
      // localStorage indisponível (ex: modo privado) — segue com carrinho vazio
    }
    setHydrated(true);
  }, []);

  useEffect(() => {
    if (!hydrated) return;
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    } catch {
      // ignora falha ao persistir
    }
  }, [items, hydrated]);

  function addItem(slug, quantity = 1) {
    setItems((prev) => {
      const existing = prev.find((item) => item.slug === slug);
      if (existing) {
        return prev.map((item) =>
          item.slug === slug
            ? { ...item, quantity: item.quantity + quantity }
            : item
        );
      }
      return [...prev, { slug, quantity }];
    });
  }

  function updateQuantity(slug, delta) {
    setItems((prev) =>
      prev.map((item) =>
        item.slug === slug
          ? { ...item, quantity: Math.max(1, item.quantity + delta) }
          : item
      )
    );
  }

  function removeItem(slug) {
    setItems((prev) => prev.filter((item) => item.slug !== slug));
  }

  const count = items.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <CartContext.Provider
      value={{ items, addItem, updateQuantity, removeItem, count }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error("useCart precisa ser usado dentro de <CartProvider>");
  }
  return context;
}
