#!/usr/bin/env bash
set -e
RG="rg-challenge-clyvo-vet"
LOCATION="chilecentral"
VNET="vnet_wise_dev"
SUBNET="sub_net_dev"
NSG="nsg_portalweb_dev"
VM="vm-wise-clyvo-dev-01"
ADMIN="azureuser"

az group create --name "$RG" --location "$LOCATION"

az network vnet create \
  --resource-group "$RG" --location "$LOCATION" \
  --name "$VNET" --address-prefixes 10.10.0.0/16 \
  --subnet-name "$SUBNET" --subnet-prefixes 10.10.1.0/24

az network nsg create \
  --resource-group "$RG" --location "$LOCATION" --name "$NSG"

az vm create \
  --resource-group "$RG" --name "$VM" \
  --image Ubuntu2204 --size Standard_B4ls_v2 \
  --admin-username "$ADMIN" --generate-ssh-keys \
  --vnet-name "$VNET" --subnet "$SUBNET" --nsg "$NSG"

az vm open-port --resource-group "$RG" --name "$VM" --port 22 --priority 1000
az vm open-port --resource-group "$RG" --name "$VM" --port 8080 --priority 1001
az vm open-port --resource-group "$RG" --name "$VM" --port 1521 --priority 1002

az vm run-command invoke \
  --resource-group "$RG" --name "$VM" \
  --command-id RunShellScript \
  --scripts "sudo apt-get update && sudo apt-get install -y git nano curl ca-certificates && curl -fsSL https://get.docker.com | sudo sh && sudo usermod -aG docker azureuser"

az vm show \
  --resource-group "$RG" --name "$VM" \
  --show-details --query publicIps --output tsv
