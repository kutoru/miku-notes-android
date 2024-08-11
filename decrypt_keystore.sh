#!/bin/sh

mkdir $HOME/secrets
gpg --quiet --batch --yes --decrypt --passphrase="$KEYSTORE_PASSPHRASE" \
    --output $HOME/secrets/androiduser.keystore androiduser.keystore.gpg
