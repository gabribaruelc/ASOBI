"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { INITIAL_PRODUCTS } from "../data/products";

const ProductsContext = createContext(null);
const STORAGE_KEY = "asobi-products";

export function ProductsProvider({ children }) {
  const [products, setProducts] = useState(INITIAL_PRODUCTS);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    try {
      const stored = window.localStorage.getItem(STORAGE_KEY);
      // eslint-disable-next-line react-hooks/set-state-in-effect -- localStorage só existe no client; lido após a hidratação para não gerar mismatch com o SSR
      if (stored) setProducts(JSON.parse(stored));
    } catch {
      // localStorage indisponível (ex: modo privado) — segue com o catálogo padrão
    }
    setHydrated(true);
  }, []);

  useEffect(() => {
    if (!hydrated) return;
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(products));
    } catch {
      // ignora falha ao persistir
    }
  }, [products, hydrated]);

  function addProduct(product) {
    setProducts((prev) => [...prev, product]);
  }

  function updateProduct(slug, patch) {
    setProducts((prev) =>
      prev.map((product) =>
        product.slug === slug ? { ...product, ...patch } : product
      )
    );
  }

  function removeProduct(slug) {
    setProducts((prev) => prev.filter((product) => product.slug !== slug));
  }

  function addReview(slug, review) {
    setProducts((prev) =>
      prev.map((product) =>
        product.slug === slug
          ? { ...product, reviews: [...product.reviews, review] }
          : product
      )
    );
  }

  function setReviewStatus(slug, reviewId, status) {
    setProducts((prev) =>
      prev.map((product) =>
        product.slug === slug
          ? {
              ...product,
              reviews: product.reviews.map((review) =>
                review.id === reviewId ? { ...review, status } : review
              ),
            }
          : product
      )
    );
  }

  function removeReview(slug, reviewId) {
    setProducts((prev) =>
      prev.map((product) =>
        product.slug === slug
          ? {
              ...product,
              reviews: product.reviews.filter(
                (review) => review.id !== reviewId
              ),
            }
          : product
      )
    );
  }

  return (
    <ProductsContext.Provider
      value={{
        products,
        hydrated,
        addProduct,
        updateProduct,
        removeProduct,
        addReview,
        setReviewStatus,
        removeReview,
      }}
    >
      {children}
    </ProductsContext.Provider>
  );
}

export function useProducts() {
  const context = useContext(ProductsContext);
  if (!context) {
    throw new Error("useProducts precisa ser usado dentro de <ProductsProvider>");
  }
  return context;
}

export function isProductNew(product) {
  return Boolean(product.newUntil) && new Date(product.newUntil) > new Date();
}

export function daysRemaining(newUntil) {
  if (!newUntil) return 0;
  const diff = new Date(newUntil).getTime() - Date.now();
  return Math.max(0, Math.ceil(diff / (1000 * 60 * 60 * 24)));
}
