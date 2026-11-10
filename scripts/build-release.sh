#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEY_DIR="$ROOT_DIR/android/keys"
KEYSTORE="$KEY_DIR/stealth-revived-release.jks"
PROPERTIES="$KEY_DIR/keystore.properties"
ALIAS="stealth-revived"

mkdir -p "$KEY_DIR"

if [[ ! -f "$KEYSTORE" ]]; then
    command -v keytool >/dev/null 2>&1 || {
        echo "keytool was not found. Install a JDK 17+ first." >&2
        exit 1
    }

    if [[ -n "${STEALTH_KEYSTORE_PASSWORD:-}" ]]; then
        STORE_PASSWORD="$STEALTH_KEYSTORE_PASSWORD"
    else
        read -r -s -p "Choose a password for your private release keystore: " STORE_PASSWORD
        printf '\n'
        read -r -s -p "Confirm keystore password: " CONFIRM_PASSWORD
        printf '\n'
        [[ "$STORE_PASSWORD" == "$CONFIRM_PASSWORD" ]] || {
            echo "Passwords did not match." >&2
            exit 1
        }
    fi

    [[ ${#STORE_PASSWORD} -ge 6 ]] || {
        echo "Use a keystore password of at least 6 characters." >&2
        exit 1
    }

    keytool -genkeypair -v \
        -keystore "$KEYSTORE" \
        -alias "$ALIAS" \
        -keyalg RSA -keysize 4096 -validity 10000 \
        -storepass "$STORE_PASSWORD" -keypass "$STORE_PASSWORD" \
        -dname "${STEALTH_KEY_DNAME:-CN=Stealth Revived, OU=Personal, O=Stealth Revived, L=Unknown, ST=Unknown, C=IN}"

    umask 077
    cat > "$PROPERTIES" <<EOF
storeFile=../keys/stealth-revived-release.jks
keyAlias=$ALIAS
storePassword=$STORE_PASSWORD
keyPassword=$STORE_PASSWORD
EOF
    chmod 600 "$KEYSTORE" "$PROPERTIES"
else
    [[ -f "$PROPERTIES" ]] || {
        echo "Keystore exists but keystore.properties is missing. Restore your saved properties file." >&2
        exit 1
    }
fi

"$ROOT_DIR/scripts/build.sh" assembleRelease
