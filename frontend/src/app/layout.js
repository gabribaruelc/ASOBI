import { Fredoka, Nunito } from "next/font/google";
import "./globals.css";
import SiteChrome from "./components/SiteChrome";
import { CartProvider } from "./context/CartContext";
import { ProductsProvider } from "./context/ProductsContext";
import { AdminProvider } from "./context/AdminContext";
import { SiteContentProvider } from "./context/SiteContentContext";

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

export default function RootLayout({ children }) {
  return (
    <html lang="pt-BR" className={`${headingFont.variable} ${bodyFont.variable}`}>
      <body>
        <ProductsProvider>
          <AdminProvider>
            <SiteContentProvider>
              <CartProvider>
                <SiteChrome>{children}</SiteChrome>
              </CartProvider>
            </SiteContentProvider>
          </AdminProvider>
        </ProductsProvider>
      </body>
    </html>
  );
}
