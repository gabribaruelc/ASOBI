import Link from "next/link";
import styles from "./PolicyPage.module.css";
import { getPolicy } from "../lib/catalog";

// O texto vem do painel (Políticas) como texto simples: linha em branco separa
// parágrafos e linha começando com "#" vira subtítulo.
function parseBody(body) {
  const blocks = [];
  let paragraph = [];
  const flush = () => {
    if (paragraph.length > 0) {
      blocks.push({ type: "paragraph", text: paragraph.join("\n") });
      paragraph = [];
    }
  };
  for (const rawLine of body.split("\n")) {
    const line = rawLine.trim();
    if (line.startsWith("#")) {
      flush();
      blocks.push({ type: "heading", text: line.replace(/^#+\s*/, "") });
    } else if (line === "") {
      flush();
    } else {
      paragraph.push(line);
    }
  }
  flush();
  return blocks;
}

/** slug: privacy, returns ou shipping. title: usado se o backend não responder. */
export default async function PolicyPage({ slug, title }) {
  const policy = await getPolicy(slug);

  return (
    <main className={styles.main}>
      <div className={`container ${styles.content}`}>
        <h1>{policy?.title || title}</h1>

        {policy ? (
          <>
            {parseBody(policy.body).map((block, index) =>
              block.type === "heading" ? (
                <h2 key={index}>{block.text}</h2>
              ) : (
                <p key={index}>{block.text}</p>
              ),
            )}
            <p className={styles.updated}>
              Atualizado em{" "}
              {new Date(policy.updatedAt).toLocaleDateString("pt-BR", {
                timeZone: "America/Sao_Paulo",
              })}
              .
            </p>
          </>
        ) : (
          <p>
            Não conseguimos carregar este texto agora. Tente de novo em instantes ou{" "}
            <Link href="/fale-conosco">fale com a gente</Link>.
          </p>
        )}
      </div>
    </main>
  );
}
