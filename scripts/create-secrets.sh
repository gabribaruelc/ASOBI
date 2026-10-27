#!/usr/bin/env bash
# Cria no Secret Manager os segredos que o deploy no Cloud Run usa (--set-secrets dos READMEs).
# Pode rodar quantas vezes quiser: segredo que já existe não é mexido.
#   - chaves internas (entre o backend e os serviços): sorteadas aqui, ninguém precisa ver;
#   - banco: lidas de backend/.env e admin-auth-service/.env;
#   - contas externas: o script pergunta; Enter em branco pula (rode de novo quando tiver).
# Uso, na raiz do repositório: bash scripts/create-secrets.sh
# Para trocar um valor depois: printf %s 'novo' | gcloud secrets versions add <nome> --data-file=-
set -euo pipefail

PROJECT="${GCP_PROJECT:-asobi-loja}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

gcloud services enable secretmanager.googleapis.com --project "$PROJECT"

exists() {
  gcloud secrets describe "$1" --project "$PROJECT" >/dev/null 2>&1
}

save() {
  printf %s "$2" | gcloud secrets create "$1" --project "$PROJECT" \
    --replication-policy automatic --data-file=- >/dev/null
  echo "  criado: $1"
}

random_secret() {
  if exists "$1"; then echo "  já existe: $1"; return; fi
  save "$1" "$(openssl rand -base64 36 | tr '+/' '-_' | tr -d '=\n')"
}

# Valor de uma variável num .env (formato .properties: "\\" vira "\").
from_env() {
  local name="$1" file="$2" key="$3" value
  if exists "$name"; then echo "  já existe: $name"; return; fi
  value="$(grep -E "^$key=" "$ROOT/$file" 2>/dev/null | head -1 | cut -d= -f2- | sed 's/\\\\/\\/g')" || true
  if [ -z "$value" ]; then echo "  pulado: $name ($key vazio em $file)"; return; fi
  save "$name" "$value"
}

ask() {
  local name="$1" label="$2" value
  if exists "$name"; then echo "  já existe: $name"; return; fi
  read -rsp "  $label (Enter pula): " value; echo
  if [ -z "$value" ]; then echo "  pulado: $name"; return; fi
  save "$name" "$value"
}

echo "Chaves internas"
random_secret asobi-shipping-api-key
random_secret asobi-admin-auth-api-key
random_secret asobi-two-factor-api-key
random_secret asobi-two-factor-code-secret
random_secret asobi-notification-api-key
random_secret asobi-jobs-api-key

echo "Banco (Supabase)"
from_env asobi-db-url backend/.env DB_URL
from_env asobi-db-user backend/.env DB_USER
from_env asobi-db-password backend/.env DB_PASSWORD
from_env asobi-admin-auth-db-user admin-auth-service/.env DB_USER
from_env asobi-admin-auth-db-password admin-auth-service/.env DB_PASSWORD
from_env asobi-two-factor-db-user two-factor-service/.env DB_USER
from_env asobi-two-factor-db-password two-factor-service/.env DB_PASSWORD
from_env asobi-notification-db-user notification-service/.env DB_USER
from_env asobi-notification-db-password notification-service/.env DB_PASSWORD

echo "Contas externas"
ask asobi-supabase-service-key "Supabase: chave secreta (service_role / sb_secret_...)"
ask asobi-google-client-id "Google OAuth: Client ID"
ask asobi-google-client-secret "Google OAuth: Client secret"
ask asobi-mp-token "Mercado Pago: Access Token de produção"
ask asobi-mp-webhook-secret "Mercado Pago: assinatura secreta do webhook"
from_env asobi-resend-api-key backend/.env RESEND_API_KEY
ask asobi-melhor-envio-token "Melhor Envio: token de produção"

# O Cloud Run roda com a conta de serviço padrão do Compute; ela precisa ler os segredos.
NUMBER="$(gcloud projects describe "$PROJECT" --format 'value(projectNumber)')"
gcloud projects add-iam-policy-binding "$PROJECT" \
  --member "serviceAccount:$NUMBER-compute@developer.gserviceaccount.com" \
  --role roles/secretmanager.secretAccessor --condition None >/dev/null
echo "Pronto. A conta de serviço do Cloud Run já pode ler os segredos."
