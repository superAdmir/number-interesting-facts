#!/bin/zsh
# Reports which upload key the local signing configuration uses and whether Play accepts it.
# The password is passed to keytool only as a file path or env-var name; it is never printed.
# Exit: 0 = accepted by Play, 10 = new key / reset pending, 1 = unknown or mismatch, 2 = not configured.
source "$(dirname "$0")/upload-key-common.sh"
CONF="$ROOT/signing.local.properties"
[ -f "$CONF" ] || { echo "Missing signing.local.properties (storeFile, keyAlias, storePasswordFile)"; exit 2; }
STORE=$(prop storeFile "$CONF"); ALIAS=$(prop keyAlias "$CONF"); PWFILE=$(prop storePasswordFile "$CONF")
[ -f "$STORE" ] || { echo "Keystore not found at storeFile path"; exit 2; }
[ -n "$ALIAS" ] || { echo "keyAlias missing"; exit 2; }
if [ -n "$NIF_UPLOAD_STORE_PASSWORD" ]; then PASSOPT=(-storepass:env NIF_UPLOAD_STORE_PASSWORD)
elif [ -f "$PWFILE" ]; then PASSOPT=(-storepass:file "$PWFILE")
else echo "No password source (storePasswordFile or NIF_UPLOAD_STORE_PASSWORD)"; exit 2; fi
SHA=$(keytool -list -v -keystore "$STORE" -alias "$ALIAS" "${PASSOPT[@]}" 2>/dev/null | sed -n 's/^[[:space:]]*SHA256: //p' | head -1)
[ -n "$SHA" ] || { echo "Could not read the alias certificate (wrong alias or password?)"; exit 1; }
echo "local signing key ($ALIAS) SHA-256: $SHA"
echo "reset status: $RESET"
classify "$SHA" "local signing key"
