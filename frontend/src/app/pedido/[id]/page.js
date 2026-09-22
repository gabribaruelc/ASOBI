"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import styles from "./page.module.css";
import { formatPrice } from "../../data/products";
import { apiFetch, isApiEnabled } from "../../lib/api";

const STATUS_MESSAGES = {
  PENDING_PAYMENT: {
    icon: "⏳",
    title: "Aguardando o pagamento",
    text: "Assim que o pagamento for confirmado, esta página atualiza sozinha. Pix costuma ser na hora; boleto pode levar até 2 dias úteis.",
  },
  PAID: {
    icon: "🎉",
    title: "Pagamento confirmado!",
    text: "Já estamos separando seus jogos com carinho. Você recebe o código de rastreio quando o pedido for enviado.",
  },
  SHIPPED: {
    icon: "🚚",
    title: "Pedido enviado!",
    text: "Seus jogos estão a caminho.",
  },
  CANCELED: {
    icon: "😕",
    title: "Pedido cancelado",
    text: "O pagamento não foi concluído. Se quiser, é só montar o carrinho de novo.",
  },
};

// Enquanto aguarda pagamento, consulta de novo a cada 10 s (por até 10 min).
const POLL_INTERVAL_MS = 10_000;
const POLL_LIMIT = 60;

export default function PedidoPage() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isApiEnabled) return;
    let polls = 0;
    let timer;
    let active = true;

    async function load() {
      try {
        const data = await apiFetch(`/api/orders/${encodeURIComponent(id)}`);
        if (!active) return;
        setOrder(data);
        if (data.status === "PENDING_PAYMENT" && polls++ < POLL_LIMIT) {
          timer = setTimeout(load, POLL_INTERVAL_MS);
        }
      } catch (err) {
        if (active) setError(err.status === 404 ? "Pedido não encontrado." : err.message);
      }
    }

    // Voltando do Mercado Pago (?payment_id=...): pede ao backend para conferir
    // o pagamento na hora, sem depender só do webhook.
    const paymentId = new URLSearchParams(window.location.search).get("payment_id");
    const sync =
      paymentId && /^\d+$/.test(paymentId)
        ? apiFetch(`/api/orders/${encodeURIComponent(id)}/payment-sync`, {
            method: "POST",
            body: JSON.stringify({ paymentId }),
          }).catch(() => {})
        : Promise.resolve();

    sync.then(load);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [id]);

  if (error) {
    return (
      <main className={styles.main}>
        <div className="container">
          <p className={styles.card}>{error}</p>
        </div>
      </main>
    );
  }

  if (!order) {
    return (
      <main className={styles.main}>
        <div className="container">
          <p className={styles.card}>Carregando seu pedido…</p>
        </div>
      </main>
    );
  }

  const message = STATUS_MESSAGES[order.status];

  return (
    <main className={styles.main}>
      <div className="container">
        <section className={`${styles.card} ${styles.hero}`}>
          <span className={styles.icon} aria-hidden="true">
            {message.icon}
          </span>
          <p className={styles.kicker}>Pedido #{order.number}</p>
          <h1>{message.title}</h1>
          <p className={styles.text}>{message.text}</p>
          {order.trackingCode && (
            <p className={styles.tracking}>
              Código de rastreio: <strong>{order.trackingCode}</strong>
            </p>
          )}
        </section>

        <section className={styles.card}>
          <h2>Itens</h2>
          <ul className={styles.items}>
            {order.items.map((item) => (
              <li key={item.slug}>
                <Link href={`/jogos/${item.slug}`}>
                  {item.quantity}× {item.name}
                </Link>
                <span>{formatPrice(item.lineTotal)}</span>
              </li>
            ))}
            <li className={styles.muted}>
              <span>Frete</span>
              <span>{order.shippingCost > 0 ? formatPrice(order.shippingCost) : "A combinar"}</span>
            </li>
            <li className={styles.total}>
              <span>Total</span>
              <span>{formatPrice(order.total)}</span>
            </li>
          </ul>
          <p className={styles.muted}>
            Guarde este link para acompanhar o pedido. Dúvidas? Fale com a gente pelo
            WhatsApp informando o número do pedido.
          </p>
        </section>

        <Link href="/jogos" className={styles.back}>
          ← Continuar comprando
        </Link>
      </div>
    </main>
  );
}
