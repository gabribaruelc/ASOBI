"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import styles from "./page.module.css";
import { formatPrice } from "../lib/products";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import { useProducts } from "../lib/useProducts";
import { apiFetch } from "../lib/api";

const EMPTY_FORM = {
  name: "",
  email: "",
  phone: "",
  postalCode: "",
  street: "",
  number: "",
  complement: "",
  district: "",
  city: "",
  state: "",
};

export default function CheckoutPage() {
  const { items, clearCart } = useCart();
  const { products, loading } = useProducts();
  const { user, getAccessToken } = useAuth();
  const [form, setForm] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [lookingUpCep, setLookingUpCep] = useState(false);
  // Frete automático (liga/desliga no painel admin).
  const [shippingEnabled, setShippingEnabled] = useState(false);
  const [shippingOptions, setShippingOptions] = useState([]);
  const [shippingOptionId, setShippingOptionId] = useState(null);
  const [shippingLoading, setShippingLoading] = useState(false);
  const [shippingError, setShippingError] = useState(null);

  useEffect(() => {
    apiFetch("/api/settings")
      .then((settings) => setShippingEnabled(settings.shippingEnabled))
      .catch(() => setShippingEnabled(false));
  }, []);

  // Logado: já começa com o nome e o e-mail da conta (dá para trocar).
  useEffect(() => {
    if (!user) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect -- a sessão só é conhecida no navegador, depois da hidratação
    setForm((prev) => ({
      ...prev,
      name: prev.name || user.name,
      email: prev.email || user.email,
    }));
  }, [user?.email]); // eslint-disable-line react-hooks/exhaustive-deps

  const cartProducts = items
    .map((item) => {
      const product = products.find((p) => p.slug === item.slug);
      return product ? { ...product, quantity: item.quantity } : null;
    })
    .filter(Boolean);
  const subtotal = cartProducts.reduce((sum, p) => sum + p.price * p.quantity, 0);
  const unavailable = cartProducts.filter((p) => !p.inStock);
  const selectedShipping = shippingOptions.find((o) => o.id === shippingOptionId);
  const total = subtotal + (selectedShipping ? selectedShipping.price : 0);
  const needsShippingChoice = shippingEnabled && !selectedShipping;

  function update(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function loadShippingOptions(postalCode) {
    setShippingLoading(true);
    setShippingError(null);
    setShippingOptions([]);
    setShippingOptionId(null);
    try {
      const options = await apiFetch("/api/shipping/quote", {
        method: "POST",
        body: JSON.stringify({
          postalCode,
          items: items.map((item) => ({ slug: item.slug, quantity: item.quantity })),
        }),
      });
      setShippingOptions(options);
      if (options.length > 0) setShippingOptionId(options[0].id);
    } catch (err) {
      setShippingError(err.message);
    } finally {
      setShippingLoading(false);
    }
  }

  // Preenche o endereço pelo CEP (ViaCEP — serviço público e gratuito) e cota o frete.
  async function handleCepBlur() {
    const digits = form.postalCode.replace(/\D/g, "");
    if (digits.length !== 8) return;
    if (shippingEnabled) loadShippingOptions(digits);
    setLookingUpCep(true);
    try {
      const response = await fetch(`https://viacep.com.br/ws/${digits}/json/`);
      const data = await response.json();
      if (!data.erro) {
        setForm((prev) => ({
          ...prev,
          street: data.logradouro || prev.street,
          district: data.bairro || prev.district,
          city: data.localidade || prev.city,
          state: data.uf || prev.state,
        }));
      }
    } catch {
      // Sem ViaCEP, a pessoa preenche o endereço à mão.
    } finally {
      setLookingUpCep(false);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      // Com login, o pedido fica na conta ("Meus pedidos"); sem login, segue sem cadastro.
      const token = await getAccessToken();
      const order = await apiFetch("/api/orders", {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: JSON.stringify({
          customer: { name: form.name, email: form.email, phone: form.phone },
          shippingAddress: {
            postalCode: form.postalCode,
            street: form.street,
            number: form.number,
            complement: form.complement,
            district: form.district,
            city: form.city,
            state: form.state,
          },
          items: items.map((item) => ({ slug: item.slug, quantity: item.quantity })),
          shippingOptionId: shippingEnabled ? shippingOptionId : null,
        }),
      });
      clearCart();
      // Vai para o Mercado Pago (ou para a página do pedido, no modo manual).
      window.location.href = order.checkoutUrl;
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors || {});
      setSubmitting(false);
    }
  }

  if (!loading && cartProducts.length === 0) {
    return (
      <main className={styles.main}>
        <div className="container">
          <p className={styles.notice}>
            Seu carrinho está vazio. <Link href="/jogos">Ver jogos</Link>
          </p>
        </div>
      </main>
    );
  }

  const fieldError = (key) =>
    fieldErrors[key] ? <small className={styles.fieldError}>{fieldErrors[key]}</small> : null;

  return (
    <main className={styles.main}>
      <div className="container">
        <h1 className={styles.title}>Finalizar compra</h1>

        <form className={styles.layout} onSubmit={handleSubmit} noValidate>
          <div className={styles.forms}>
            <section className={styles.card}>
              <h2>Seus dados</h2>
              <p className={styles.hint}>
                Dados do adulto responsável pela compra. Usamos só para entregar o
                pedido e falar com você sobre ele.
              </p>
              {user && (
                <p className={styles.hint}>
                  Comprando como <strong>{user.email}</strong> — este pedido vai
                  aparecer em <Link href="/conta">Meus pedidos</Link>.
                </p>
              )}
              <label className={styles.field}>
                <span>Nome completo</span>
                <input
                  value={form.name}
                  onChange={(e) => update("name", e.target.value)}
                  autoComplete="name"
                  maxLength={120}
                  required
                />
                {fieldError("customer.name")}
              </label>
              <div className={styles.row}>
                <label className={styles.field}>
                  <span>E-mail</span>
                  <input
                    type="email"
                    value={form.email}
                    onChange={(e) => update("email", e.target.value)}
                    autoComplete="email"
                    required
                  />
                  {fieldError("customer.email")}
                </label>
                <label className={styles.field}>
                  <span>Telefone / WhatsApp</span>
                  <input
                    type="tel"
                    value={form.phone}
                    onChange={(e) => update("phone", e.target.value)}
                    autoComplete="tel"
                    placeholder="(11) 98765-4321"
                    required
                  />
                  {fieldError("customer.phone")}
                </label>
              </div>
            </section>

            <section className={styles.card}>
              <h2>Endereço de entrega</h2>
              <div className={styles.row}>
                <label className={styles.field}>
                  <span>CEP {lookingUpCep && <em>buscando…</em>}</span>
                  <input
                    inputMode="numeric"
                    value={form.postalCode}
                    onChange={(e) => update("postalCode", e.target.value)}
                    onBlur={handleCepBlur}
                    autoComplete="postal-code"
                    placeholder="00000-000"
                    maxLength={9}
                    required
                  />
                  {fieldError("shippingAddress.postalCode")}
                </label>
                <label className={styles.field}>
                  <span>Número</span>
                  <input
                    value={form.number}
                    onChange={(e) => update("number", e.target.value)}
                    maxLength={20}
                    required
                  />
                  {fieldError("shippingAddress.number")}
                </label>
              </div>
              <label className={styles.field}>
                <span>Rua</span>
                <input
                  value={form.street}
                  onChange={(e) => update("street", e.target.value)}
                  autoComplete="address-line1"
                  maxLength={200}
                  required
                />
                {fieldError("shippingAddress.street")}
              </label>
              <div className={styles.row}>
                <label className={styles.field}>
                  <span>Complemento (opcional)</span>
                  <input
                    value={form.complement}
                    onChange={(e) => update("complement", e.target.value)}
                    autoComplete="address-line2"
                    maxLength={100}
                  />
                </label>
                <label className={styles.field}>
                  <span>Bairro</span>
                  <input
                    value={form.district}
                    onChange={(e) => update("district", e.target.value)}
                    maxLength={100}
                    required
                  />
                  {fieldError("shippingAddress.district")}
                </label>
              </div>
              <div className={styles.row}>
                <label className={styles.field}>
                  <span>Cidade</span>
                  <input
                    value={form.city}
                    onChange={(e) => update("city", e.target.value)}
                    autoComplete="address-level2"
                    maxLength={100}
                    required
                  />
                  {fieldError("shippingAddress.city")}
                </label>
                <label className={styles.field}>
                  <span>UF</span>
                  <input
                    value={form.state}
                    onChange={(e) => update("state", e.target.value.toUpperCase())}
                    autoComplete="address-level1"
                    maxLength={2}
                    placeholder="SP"
                    required
                  />
                  {fieldError("shippingAddress.state")}
                </label>
              </div>
            </section>

            {shippingEnabled && (
              <section className={styles.card}>
                <h2>Frete</h2>
                {shippingLoading && <p className={styles.hint}>Calculando o frete…</p>}
                {!shippingLoading && shippingOptions.length === 0 && !shippingError && (
                  <p className={styles.hint}>Informe o CEP para ver as opções de entrega.</p>
                )}
                {shippingError && <p className={styles.error}>{shippingError}</p>}
                <div className={styles.shippingOptions}>
                  {shippingOptions.map((option) => (
                    <label key={option.id} className={styles.shippingOption}>
                      <input
                        type="radio"
                        name="shippingOption"
                        value={option.id}
                        checked={shippingOptionId === option.id}
                        onChange={() => setShippingOptionId(option.id)}
                      />
                      <span className={styles.shippingName}>
                        {option.company} {option.name}
                        {option.deliveryDays != null && (
                          <small> · até {option.deliveryDays} dias úteis</small>
                        )}
                      </span>
                      <span className={styles.shippingPrice}>
                        {option.price === 0 ? (
                          <>
                            {option.originalPrice && <s>{formatPrice(option.originalPrice)}</s>} Grátis
                          </>
                        ) : (
                          formatPrice(option.price)
                        )}
                      </span>
                    </label>
                  ))}
                </div>
              </section>
            )}
          </div>

          <aside className={styles.summary}>
            <h2>Resumo do pedido</h2>
            <ul className={styles.itemList}>
              {cartProducts.map((product) => (
                <li key={product.slug}>
                  <span>
                    {product.quantity}× {product.name}
                    {!product.inStock && <strong className={styles.soldOut}> esgotado</strong>}
                  </span>
                  <span>{formatPrice(product.price * product.quantity)}</span>
                </li>
              ))}
            </ul>
            <div className={styles.summaryRow}>
              <span>Subtotal</span>
              <span>{formatPrice(subtotal)}</span>
            </div>
            <div className={styles.summaryRow}>
              <span>Frete</span>
              {!shippingEnabled ? (
                <span className={styles.muted}>A combinar</span>
              ) : selectedShipping ? (
                <span>{selectedShipping.price === 0 ? "Grátis" : formatPrice(selectedShipping.price)}</span>
              ) : (
                <span className={styles.muted}>Informe o CEP</span>
              )}
            </div>
            <div className={`${styles.summaryRow} ${styles.summaryTotal}`}>
              <span>Total</span>
              <span>{formatPrice(total)}</span>
            </div>

            {error && (
              <p className={styles.error} role="alert">
                {error}
              </p>
            )}
            {unavailable.length > 0 && (
              <p className={styles.error}>
                Há itens esgotados no carrinho. <Link href="/carrinho">Ajustar carrinho</Link>
              </p>
            )}

            <button
              type="submit"
              className={styles.submit}
              disabled={submitting || loading || unavailable.length > 0 || needsShippingChoice}
            >
              {submitting ? "Enviando…" : "Ir para o pagamento"}
            </button>
            <p className={styles.muted}>
              Pagamento seguro pelo Mercado Pago: Pix, cartão ou boleto. Não guardamos
              dados de cartão.
            </p>
            <p className={styles.lgpd}>
              🔒 Não pedimos nenhum dado das crianças — só do adulto que compra (LGPD).
            </p>
          </aside>
        </form>
      </div>
    </main>
  );
}
