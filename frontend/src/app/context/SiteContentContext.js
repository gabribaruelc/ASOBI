"use client";

import { createContext, useContext, useEffect, useState } from "react";

const SiteContentContext = createContext(null);
const STORAGE_KEY = "asobi-site-content";

const DEFAULT_SOBRE = {
  heroText:
    'O nome ASOBI vem do japonês 遊び ("asobi"), que significa brincadeira. Acreditamos que é brincando que as crianças aprendem melhor — e é por isso que existimos: para levar até as famílias jogos de tabuleiro que unem diversão de verdade com desenvolvimento infantil.',
  values: [
    {
      icon: "🎲",
      title: "Aprender brincando",
      text: "Selecionamos jogos que unem diversão de verdade com estímulo ao desenvolvimento infantil.",
    },
    {
      icon: "🔎",
      title: "Curadoria cuidadosa",
      text: "Cada jogo do catálogo é escolhido a dedo, com idade recomendada e habilidade estimulada bem claras.",
    },
    {
      icon: "👨‍👩‍👧",
      title: "Feito para famílias",
      text: "Queremos facilitar a vida de quem está escolhendo o presente certo para uma criança.",
    },
    {
      icon: "🛡️",
      title: "Segurança em primeiro lugar",
      text: "Cuidamos dos dados dos responsáveis com atenção especial por atender o público infantil.",
    },
  ],
  missionEmoji: "🎲",
  missionTitle: "Ajudar cada família a escolher o jogo certo",
  missionText:
    "Em cada produto do nosso catálogo, mostramos a idade recomendada, o número de jogadores e a habilidade que aquele jogo estimula — raciocínio lógico, trabalho em equipe, coordenação motora, criatividade e muito mais. Assim, escolher o presente certo fica mais simples e divertido.",
};

export function SiteContentProvider({ children }) {
  const [sobre, setSobre] = useState(DEFAULT_SOBRE);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    try {
      const stored = window.localStorage.getItem(STORAGE_KEY);
      // eslint-disable-next-line react-hooks/set-state-in-effect -- localStorage só existe no client; lido após a hidratação para não gerar mismatch com o SSR
      if (stored) setSobre(JSON.parse(stored));
    } catch {
      // localStorage indisponível — segue com o texto padrão
    }
    setHydrated(true);
  }, []);

  useEffect(() => {
    if (!hydrated) return;
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(sobre));
    } catch {
      // ignora falha ao persistir
    }
  }, [sobre, hydrated]);

  function updateSobre(patch) {
    setSobre((prev) => ({ ...prev, ...patch }));
  }

  return (
    <SiteContentContext.Provider value={{ sobre, updateSobre, hydrated }}>
      {children}
    </SiteContentContext.Provider>
  );
}

export function useSiteContent() {
  const context = useContext(SiteContentContext);
  if (!context) {
    throw new Error("useSiteContent precisa ser usado dentro de <SiteContentProvider>");
  }
  return context;
}
