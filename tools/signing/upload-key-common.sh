# Shared helpers (sourced). Reads public fingerprints and the reset status; never handles secrets
# except by passing a password file/env var name to keytool.
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
STATUS_FILE="$ROOT/tools/signing/upload-key-status.properties"
prop() { sed -n "s/^$1=//p" "$2" | head -1; }
REGISTERED=$(prop REGISTERED_UPLOAD_SHA256 "$STATUS_FILE")
NEW=$(prop NEW_UPLOAD_SHA256 "$STATUS_FILE")
RESET=$(prop RESET_STATUS "$STATUS_FILE")
# Classifies a certificate SHA-256. Exit codes: 0 accepted by Play, 10 new key with reset pending, 1 unknown.
classify() {
  local sha="$1" what="$2"
  if [ "$sha" = "$REGISTERED" ]; then
    echo "$what = currently REGISTERED Play upload certificate -> accepted by Play"; return 0
  elif [ "$sha" = "$NEW" ] && [ "$RESET" = "confirmed" ]; then
    echo "$what = NEW upload key, reset CONFIRMED by Google -> accepted by Play"; return 0
  elif [ "$sha" = "$NEW" ]; then
    echo "$what = NEW upload key (local). Play upload-key reset PENDING: this does NOT match the"
    echo "  registered Play upload certificate ($REGISTERED); Play rejects it until Google confirms the reset."
    return 10
  else
    echo "$what = UNKNOWN certificate: matches neither the registered nor the new upload key"; return 1
  fi
}
