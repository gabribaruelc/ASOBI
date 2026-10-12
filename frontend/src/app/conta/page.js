"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import styles from "./page.module.css";
import GoogleButton from "../components/GoogleButton";
import { useAuth } from "../context/AuthContext";
import { apiFetch } from "../lib/api";
import { formatPrice } from "../lib/products";

const dateFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" });

export default function ContaPage() {
  const { user, loading: authLoading, signOut, getAccessToken } = useAuth();
  const [orders, setOrders] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!user) return;
    let active = true;
    getAccessToken()
      .then((token) =>
        apiFetch("/api/account/orders", {
          headers: { Authorization: `Bearer ${token}` },
        })
      )
      .then((data) => {
        if (active) setOrders(data);
      })
      .catch((err) => {
        if (active) setError(err.message);
      });
    return () => {
      active = false;
    };
    // getAccessToken muda a cada render; o que importa é quem está logado.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.email]);

  if (authLoading) {
    return (
      <main className={styles.main}>
        <div className="container">
          <p>Carregando…</p>
        </div>
      </main>
    );
  }

  if (!user) {
    return (
      <main className={styles.main}>
        <div className={`container ${styles.signedOut}`}>
          <h1 className={styles.title}>Meus pedidos</h1>
          <p>Entre para ver os pedidos feitos com a sua conta.</p>
          <GoogleButton label="Continuar com Google" />
        </div>
      </main>
    );
  }

  return (
    <main className={styles.main}>
      <div className="container">
        <div className={styles.head}>
          <div>
            <h1 className={styles.title}>Meus pedidos</h1>
            <p className={styles.account}>
              {user.name ? `${user.name} · ` : ""}
              {user.email}
            </p>
          </div>
          <button type="button" className={styles.signOut} onClick={signOut}>
            Sair
          </button>
        </div>

        {error && <p role="alert">{error}</p>}
        {!error && orders === null && <p>Carregando seus pedidos…</p>}

        {orders?.length === 0 && (
          <div className={styles.empty}>
            <span aria-hidden="true">📦</span>
            <p>Você ainda não fez nenhum pedido.</p>
            <Link href="/jogos" className={styles.primaryLink}>
              Ver jogos
            </Link>
          </div>
        )}

        {orders?.length > 0 && (
          <ul className={styles.orders}>
            {orders.map((order) => (
              <li key={order.orderId} className={styles.order}>
                <div className={styles.orderHead}>
                  <strong>Pedido nº {order.number}</strong>
                  <span className={styles.status} data-status={order.status}>
                    {order.statusLabel}
                  </span>
                </div>
                <p className={styles.orderMeta}>
                  {dateFormat.format(new Date(order.createdAt))} ·{" "}
                  {formatPrice(order.total)}
                </p>
                <p className={styles.orderItems}>
                  {order.items
                    .map((item) => `${item.quantity}× ${item.name}`)
                    .join(", ")}
                </p>
                {order.trackingCode && (
                  <p className={styles.orderMeta}>
                    Código de rastreio: <strong>{order.trackingCode}</strong>
                  </p>
                )}
                <Link href={`/pedido/${order.orderId}`} className={styles.orderLink}>
                  Ver detalhes
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </main>
  );
}
