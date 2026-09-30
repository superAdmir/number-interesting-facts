#!/bin/zsh
# Verifies a release bundle: signature, signer classification (registered / new-pending / unknown),
# version metadata, package, production AdMob app ID, absence of debug/QA hooks.
# Exit: 0 = upload-ready signer, 10 = signed with NEW key, Play reset pending, 1 = problem.
source "$(dirname "$0")/upload-key-common.sh"
cd "$ROOT"
AAB=${1:-app/build/outputs/bundle/release/app-release.aab}
echo "bundle: $ROOT/$AAB"
# Android upload certificates are self-signed and Play uses no timestamp authority, so jarsigner's
# chain/timestamp warnings are expected; the signature itself must verify ("jar verified.").
JS=$(jarsigner -verify "$AAB" 2>&1)
echo "$JS" | grep -q '^jar verified\.' && echo "jarsigner: jar verified (expected warnings: self-signed certificate, no timestamp)" \
  || { echo "jarsigner: NOT verified"; exit 1; }
SIGNER=$(keytool -printcert -jarfile "$AAB" | sed -n 's/^[[:space:]]*SHA256: //p' | head -1)
echo "signer SHA-256: $SIGNER"
classify "$SIGNER" "bundle signer"; SIGN_RC=$?
[ $SIGN_RC -eq 1 ] && exit 1
TMP=$(mktemp -d)
unzip -q -o "$AAB" base/manifest/AndroidManifest.xml 'base/dex/*' -d "$TMP"
# Package: the entry not followed by ".Something" (component names are longer).
strings -n 4 "$TMP/base/manifest/AndroidManifest.xml" | grep -qE '^com\.nip\.numberinterestingfacts([^.A-Za-z0-9_]|$)' \
  && echo "package: com.nip.numberinterestingfacts" || { echo "PACKAGE MISMATCH"; exit 1; }
# Version via bundletool when available (BUNDLETOOL_JAR=/path/to/bundletool.jar).
if [ -n "$BUNDLETOOL_JAR" ] && [ -f "$BUNDLETOOL_JAR" ]; then
  java -jar "$BUNDLETOOL_JAR" validate --bundle="$AAB" >/dev/null && echo "bundletool validate: OK" || { echo "bundletool validate FAILED"; exit 1; }
  echo "versionCode: $(java -jar "$BUNDLETOOL_JAR" dump manifest --bundle="$AAB" --xpath /manifest/@android:versionCode)"
  echo "versionName: $(java -jar "$BUNDLETOOL_JAR" dump manifest --bundle="$AAB" --xpath /manifest/@android:versionName)"
fi
strings -n 4 "$TMP/base/manifest/AndroidManifest.xml" | grep -q 'ca-app-pub-6402675413704299~8756323389' && echo "production AdMob app ID: present" || { echo "PRODUCTION ADMOB APP ID MISSING"; exit 1; }
if cat "$TMP"/base/dex/*.dex | LC_ALL=C grep -q -a -e 'DebugOverrides' -e 'ca-app-pub-3940256099942544'; then
  echo "DEBUG HOOKS OR SAMPLE AD IDS FOUND"; exit 1; else echo "debug hooks / sample ad IDs: none"; fi
rm -rf "${TMP:?}"
shasum -a 256 "$AAB"
if [ $SIGN_RC -eq 10 ]; then echo "RESULT: signed with new upload key - Play reset pending (NOT upload-ready)"; exit 10; fi
echo "RESULT: signer accepted by Play"
