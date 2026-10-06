"use client";

import { useState } from "react";
import styles from "./ProductGallery.module.css";

// Galeria da ficha de produto: foto grande + miniaturas. Sem foto cadastrada,
// mostra o emoji do produto. `children` são os selos (Novo, -20%) sobre a foto.
export default function ProductGallery({ images = [], icon, name, children }) {
  const [selected, setSelected] = useState(0);
  const current = images[selected];

  return (
    <div className={styles.gallery}>
      <div className={styles.main}>
        {current ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={current} alt={name} className={styles.photo} />
        ) : (
          <span aria-hidden="true">{icon}</span>
        )}
        {children}
      </div>

      {images.length > 1 && (
        <div className={styles.thumbs}>
          {images.map((image, index) => (
            <button
              key={image}
              type="button"
              className={`${styles.thumb} ${
                index === selected ? styles.thumbActive : ""
              }`}
              onClick={() => setSelected(index)}
              aria-label={`Ver foto ${index + 1} de ${images.length}`}
              aria-pressed={index === selected}
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={image} alt="" loading="lazy" />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
