#!/usr/bin/env bash
# Cria (ou atualiza) a rotina de hora em hora do lembrete de pagamento, com a JOBS_API_KEY
# lida do Secret Manager. Rode depois do deploy do backend.
# Uso: bash scripts/create-scheduler-job.sh https://asobi-backend-xxxx.a.run.app
set -euo pipefail

PROJECT="${GCP_PROJECT:-asobi-loja}"
REGION="southamerica-east1"
JOB="asobi-payment-reminders"
BACKEND_URL="${1:?informe a URL do backend no Cloud Run}"
BACKEND_URL="${BACKEND_URL%/}"

gcloud services enable cloudscheduler.googleapis.com --project "$PROJECT"
KEY="$(gcloud secrets versions access latest --secret asobi-jobs-api-key --project "$PROJECT")"

ACTION=create
if gcloud scheduler jobs describe "$JOB" --location "$REGION" --project "$PROJECT" >/dev/null 2>&1; then
  ACTION=update
fi

HEADER_FLAG=--headers
[ "$ACTION" = update ] && HEADER_FLAG=--update-headers

gcloud scheduler jobs "$ACTION" http "$JOB" \
  --project "$PROJECT" --location "$REGION" \
  --schedule "0 * * * *" --time-zone "America/Sao_Paulo" \
  --uri "$BACKEND_URL/api/jobs/payment-reminders" \
  --http-method POST \
  "$HEADER_FLAG" "X-Job-Key=$KEY" >/dev/null

echo "Rotina $JOB: $ACTION ok. Para testar agora:"
echo "  gcloud scheduler jobs run $JOB --location $REGION --project $PROJECT"
