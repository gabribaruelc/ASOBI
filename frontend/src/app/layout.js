import { Fredoka, Nunito } from "next/font/google";
import "./globals.css";
import Header from "./components/Header";
import Footer from "./components/Footer";
import { CartProvider } from "./context/CartContext";
import { AuthProvider } from "./context/AuthContext";
import { getAgeFilters, getSiteInfo, getStoreSettings } from "./lib/catalog";

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
  const [ageFilters, siteInfo, settings] = await Promise.all([
    getAgeFilters(),
    getSiteInfo(),
    getStoreSettings(),
  ]);

  return (
    <html lang="pt-BR" className={`${headingFont.variable} ${bodyFont.variable}`}>
      <body>
        <AuthProvider>
          <CartProvider>
            <Header
              ageFilters={ageFilters}
              freeShippingThreshold={settings.freeShippingThreshold ?? null}
            />
            {children}
            <Footer siteInfo={siteInfo} />
          </CartProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
