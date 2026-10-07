#!/usr/bin/env bash
set -euo pipefail

GROUP="rg-vetsync"
LOCATION="chilecentral"
SKU_DEV="B1"
PLAN="plan-vetsync"
APP_NAME="app-vetsync-rm563197"
KEYVAULT_NAME="kv-vetsync-rm563197"
DEPLOY_IDENTITY_NAME="id-vetsync-github"
GITHUB_REPOSITORY="Challenge-2TDSPG-2026/vetSync-java"
FEDERATED_CREDENTIAL_NAME="github-main-vetsync-java"

az provider register --namespace Microsoft.Resources --wait
az provider register --namespace Microsoft.Web --wait
az provider register --namespace Microsoft.KeyVault --wait
az provider register --namespace Microsoft.ManagedIdentity --wait

az group create \
    --location "$LOCATION" \
    --name "$GROUP"

if ! az keyvault show --resource-group "$GROUP" --name "$KEYVAULT_NAME" >/dev/null 2>&1; then
    az keyvault create \
        --resource-group "$GROUP" \
        --name "$KEYVAULT_NAME" \
        --location "$LOCATION" \
        --enable-rbac-authorization true \
        --output none
fi

CURRENT_USER_OBJECT_ID=$(az ad signed-in-user show --query id --output tsv)
KEYVAULT_ID=$(az keyvault show \
    --resource-group "$GROUP" \
    --name "$KEYVAULT_NAME" \
    --query id \
    --output tsv)

if ! az role assignment list \
    --assignee-object-id "$CURRENT_USER_OBJECT_ID" \
    --scope "$KEYVAULT_ID" \
    --role "Key Vault Secrets Officer" \
    --query "[0].id" --output tsv | grep -q .; then
    az role assignment create \
        --assignee-object-id "$CURRENT_USER_OBJECT_ID" \
        --assignee-principal-type User \
        --role "Key Vault Secrets Officer" \
        --scope "$KEYVAULT_ID" \
        --output none
fi

if ! az appservice plan show --resource-group "$GROUP" --name "$PLAN" >/dev/null 2>&1; then
    az appservice plan create \
        --resource-group "$GROUP" \
        --name "$PLAN" \
        --sku "$SKU_DEV" \
        --is-linux \
        --output none
fi

if ! az webapp show --resource-group "$GROUP" --name "$APP_NAME" >/dev/null 2>&1; then
    az webapp create \
        --resource-group "$GROUP" \
        --plan "$PLAN" \
        --name "$APP_NAME" \
        --runtime "JAVA:17-java17" \
        --output none
fi

az webapp identity assign \
    --resource-group "$GROUP" \
    --name "$APP_NAME" \
    --output none

if ! az identity show --resource-group "$GROUP" --name "$DEPLOY_IDENTITY_NAME" >/dev/null 2>&1; then
    az identity create \
        --resource-group "$GROUP" \
        --name "$DEPLOY_IDENTITY_NAME" \
        --location "$LOCATION" \
        --output none
fi

DEPLOY_PRINCIPAL_ID=$(az identity show \
    --resource-group "$GROUP" \
    --name "$DEPLOY_IDENTITY_NAME" \
    --query principalId \
    --output tsv)

APP_ID=$(az webapp show \
    --resource-group "$GROUP" \
    --name "$APP_NAME" \
    --query id \
    --output tsv)

if ! az role assignment list \
    --assignee-object-id "$DEPLOY_PRINCIPAL_ID" \
    --scope "$APP_ID" \
    --role "Website Contributor" \
    --query "[0].id" --output tsv | grep -q .; then
    az role assignment create \
        --assignee-object-id "$DEPLOY_PRINCIPAL_ID" \
        --assignee-principal-type ServicePrincipal \
        --role "Website Contributor" \
        --scope "$APP_ID" \
        --output none
fi

if ! az identity federated-credential show \
    --resource-group "$GROUP" \
    --identity-name "$DEPLOY_IDENTITY_NAME" \
    --name "$FEDERATED_CREDENTIAL_NAME" >/dev/null 2>&1; then
    az identity federated-credential create \
        --resource-group "$GROUP" \
        --identity-name "$DEPLOY_IDENTITY_NAME" \
        --name "$FEDERATED_CREDENTIAL_NAME" \
        --issuer "https://token.actions.githubusercontent.com" \
        --subject "repo:$GITHUB_REPOSITORY:ref:refs/heads/main" \
        --audiences "api://AzureADTokenExchange" \
        --output none
fi

az webapp config set \
    --resource-group "$GROUP" \
    --name "$APP_NAME" \
    --always-on true \
    --generic-configurations '{"healthCheckPath":"/actuator/health"}' \
    --output none

az webapp log config \
    --resource-group "$GROUP" \
    --name "$APP_NAME" \
    --application-logging filesystem \
    --level information \
    --web-server-logging filesystem \
    --output none

echo "Azure resources and GitHub OIDC identity are ready."
