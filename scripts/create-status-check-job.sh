#!/usr/bin/env bash
# Cria (ou atualiza) a rotina de 5 em 5 minutos que chama o status-service, com a
# STATUS_SERVICE_API_KEY lida do Secret Manager. Rode depois do deploy do status-service.
# Uso: bash scripts/create-status-check-job.sh https://asobi-status-service-xxxx.a.run.app
set -euo pipefail

PROJECT="${GCP_PROJECT:-asobi-loja}"
REGION="southamerica-east1"
JOB="asobi-status-checks"
SERVICE_URL="${1:?informe a URL do status-service no Cloud Run}"
SERVICE_URL="${SERVICE_URL%/}"

gcloud services enable cloudscheduler.googleapis.com --project "$PROJECT"
KEY="$(gcloud secrets versions access latest --secret asobi-status-api-key --project "$PROJECT")"

ACTION=create
if gcloud scheduler jobs describe "$JOB" --location "$REGION" --project "$PROJECT" >/dev/null 2>&1; then
  ACTION=update
fi

HEADER_FLAG=--headers
[ "$ACTION" = update ] && HEADER_FLAG=--update-headers

# --attempt-deadline: os serviços podem estar "acordando"; cada conferência espera até 30 s
gcloud scheduler jobs "$ACTION" http "$JOB" \
  --project "$PROJECT" --location "$REGION" \
  --schedule "*/5 * * * *" --time-zone "America/Sao_Paulo" \
  --uri "$SERVICE_URL/api/checks" \
  --http-method POST \
  --attempt-deadline 120s \
  "$HEADER_FLAG" "X-Api-Key=$KEY" >/dev/null

echo "Rotina $JOB: $ACTION ok. Para testar agora:"
echo "  gcloud scheduler jobs run $JOB --location $REGION --project $PROJECT"
