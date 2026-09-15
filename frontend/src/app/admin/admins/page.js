"use client";

import { useState } from "react";
import styles from "../admin.module.css";
import { useAdmin } from "../../context/AdminContext";

export default function AdminAdminsPage() {
  const { admins, addAdmin, removeAdmin, currentEmail } = useAdmin();
  const [email, setEmail] = useState("");

  function handleAdd(event) {
    event.preventDefault();
    if (!email.trim()) return;
    addAdmin(email);
    setEmail("");
  }

  function handleRemove(admin) {
    if (window.confirm(`Remover o acesso de admin de "${admin}"?`)) {
      removeAdmin(admin);
    }
  }

  return (
    <div>
      <h1 className={styles.pageTitle}>Admins</h1>
      <p className={styles.pageDesc} style={{ marginBottom: "24px" }}>
        Quem estiver nessa lista consegue entrar no painel com esse e-mail.
      </p>

      <form className={styles.inlineForm} onSubmit={handleAdd}>
        <input
          type="email"
          placeholder="novo-admin@email.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <button type="submit" className={styles.primaryButton}>
          Adicionar admin
        </button>
      </form>

      <ul className={styles.adminsList}>
        {admins.map((admin) => (
          <li key={admin}>
            <span>{admin}</span>
            {admin !== currentEmail && (
              <button type="button" onClick={() => handleRemove(admin)}>
                Remover
              </button>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
