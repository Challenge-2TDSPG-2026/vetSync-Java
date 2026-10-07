#!/usr/bin/env bash
set -euo pipefail

GROUP="rg-vetsync"

echo "Deleting resource group: $GROUP"

az group delete \
    --name "$GROUP" \
    --yes \
    --no-wait
