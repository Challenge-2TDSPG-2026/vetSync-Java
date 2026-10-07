#!/usr/bin/env bash
set -euo pipefail

GROUP="rg-vetsync"
KEYVAULT_NAME="kv-vetsync-rm563197"
APP_NAME="app-vetsync-rm563197"
ORACLE_JDBC_URL="${ORACLE_JDBC_URL:-jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL}"

if [[ -z "${ORACLE_FIAP_USERNAME:-}" ]]; then
    read -rp "Usuario Oracle FIAP (ex.: RM563197): " ORACLE_FIAP_USERNAME
fi

if [[ -z "${ORACLE_FIAP_PASSWORD:-}" ]]; then
    read -rsp "Senha Oracle FIAP: " ORACLE_FIAP_PASSWORD
    echo
fi

if [[ -z "$ORACLE_FIAP_USERNAME" || -z "$ORACLE_FIAP_PASSWORD" ]]; then
    echo "Usuario e senha do Oracle FIAP sao obrigatorios." >&2
    exit 1
fi

JWT_SECRET="${JWT_SECRET:-$(openssl rand -hex 48)}"
ADMIN_BOOTSTRAP_KEY="${ADMIN_BOOTSTRAP_KEY:-$(openssl rand -hex 32)}"

az keyvault secret set --vault-name "$KEYVAULT_NAME" --name "oracle-fiap-username" --value "$ORACLE_FIAP_USERNAME" --output none
az keyvault secret set --vault-name "$KEYVAULT_NAME" --name "oracle-fiap-password" --value "$ORACLE_FIAP_PASSWORD" --output none
az keyvault secret set --vault-name "$KEYVAULT_NAME" --name "jwt-secret" --value "$JWT_SECRET" --output none
az keyvault secret set --vault-name "$KEYVAULT_NAME" --name "admin-bootstrap-key" --value "$ADMIN_BOOTSTRAP_KEY" --output none

APP_PRINCIPAL_ID=$(az webapp identity show \
    --resource-group "$GROUP" \
    --name "$APP_NAME" \
    --query principalId \
    --output tsv)

KEYVAULT_ID=$(az keyvault show \
    --resource-group "$GROUP" \
    --name "$KEYVAULT_NAME" \
    --query id \
    --output tsv)

if ! az role assignment list \
    --assignee-object-id "$APP_PRINCIPAL_ID" \
    --scope "$KEYVAULT_ID" \
    --role "Key Vault Secrets User" \
    --query "[0].id" --output tsv | grep -q .; then
    az role assignment create \
        --assignee-object-id "$APP_PRINCIPAL_ID" \
        --assignee-principal-type ServicePrincipal \
        --role "Key Vault Secrets User" \
        --scope "$KEYVAULT_ID" \
        --output none
fi

az webapp config appsettings set \
    --resource-group "$GROUP" \
    --name "$APP_NAME" \
    --settings \
    "SPRING_DATASOURCE_URL=$ORACLE_JDBC_URL" \
    "SPRING_DATASOURCE_USERNAME=@Microsoft.KeyVault(VaultName=$KEYVAULT_NAME;SecretName=oracle-fiap-username)" \
    "DB_PASSWORD=@Microsoft.KeyVault(VaultName=$KEYVAULT_NAME;SecretName=oracle-fiap-password)" \
    "JWT_SECRET=@Microsoft.KeyVault(VaultName=$KEYVAULT_NAME;SecretName=jwt-secret)" \
    "ADMIN_BOOTSTRAP_KEY=@Microsoft.KeyVault(VaultName=$KEYVAULT_NAME;SecretName=admin-bootstrap-key)" \
    "SPRING_FLYWAY_ENABLED=true" \
    "MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=never" \
    "SERVER_PORT=8080" \
    "WEBSITES_PORT=8080" \
    "WEBSITE_HEALTHCHECK_MAXPINGFAILURES=10" \
    --output none

unset ORACLE_FIAP_PASSWORD JWT_SECRET ADMIN_BOOTSTRAP_KEY

echo "Oracle FIAP and application secrets configured in the App Service."
