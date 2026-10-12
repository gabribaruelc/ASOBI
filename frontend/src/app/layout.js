import { Fredoka, Nunito } from "next/font/google";
import "./globals.css";
import Header from "./components/Header";
import Footer from "./components/Footer";
import { CartProvider } from "./context/CartContext";
import { AuthProvider } from "./context/AuthContext";
import { getAgeFilters } from "./lib/catalog";

const headingFont = Fredoka({
  variable: "--font-heading",
  subsets: ["latin"],
  weight: ["500", "600", "700"],
});

const bodyFont = Nunito({
  variable: "--font-body",
  subsets: ["latin"],
  weight: ["400", "600", "700"],
});

export const metadata = {
  title: "ASOBI | Jogos de tabuleiro infantis",
  description:
    "Jogos de tabuleiro que estimulam o aprendizado infantil pela brincadeira.",
};

export default async function RootLayout({ children }) {
  const ageFilters = await getAgeFilters();

  return (
    <html lang="pt-BR" className={`${headingFont.variable} ${bodyFont.variable}`}>
      <body>
        <AuthProvider>
          <CartProvider>
            <Header ageFilters={ageFilters} />
            {children}
            <Footer />
          </CartProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
