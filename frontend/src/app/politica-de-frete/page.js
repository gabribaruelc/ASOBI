import PolicyPage from "../components/PolicyPage";

export const metadata = {
  title: "Política de Frete | ASOBI",
  description: "Como funcionam o cálculo do frete, os prazos e o acompanhamento da entrega na ASOBI.",
};

export default function PoliticaDeFretePage() {
  return <PolicyPage slug="shipping" title="Política de Frete" />;
}
