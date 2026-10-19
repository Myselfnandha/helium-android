#!/usr/bin/env python3
import os, sys, base64, secrets, string, subprocess, urllib.parse, urllib.request, json
import nacl.encoding
import nacl.public

def get_github_token():
    creds_file = os.path.expanduser("~/.git-credentials")
    if not os.path.exists(creds_file):
        return None
    with open(creds_file) as f:
        for line in f:
            if "github.com" in line:
                return urllib.parse.urlparse(line.strip()).password
    return None

def generate_random_password(length=24):
    alphabet = string.ascii_letters + string.digits
    return "".join(secrets.choice(alphabet) for _ in range(length))

def encrypt_secret(public_key_b64: str, secret_value: str) -> str:
    public_key = nacl.public.PublicKey(public_key_b64.encode("utf-8"), nacl.encoding.Base64Encoder)
    sealed_box = nacl.public.SealedBox(public_key)
    encrypted = sealed_box.encrypt(secret_value.encode("utf-8"))
    return base64.b64encode(encrypted).decode("utf-8")

def upload_secret(repo: str, token: str, secret_name: str, encrypted_val: str, key_id: str):
    url = f"https://api.github.com/repos/{repo}/actions/secrets/{secret_name}"
    payload = json.dumps({
        "encrypted_value": encrypted_val,
        "key_id": key_id
    }).encode("utf-8")

    req = urllib.request.Request(
        url,
        data=payload,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github.v3+json",
            "Content-Type": "application/json",
            "User-Agent": "Helium-KeySetup"
        },
        method="PUT"
    )
    with urllib.request.urlopen(req) as resp:
        if resp.status in (201, 204):
            print(f"  ✓ Secret '{secret_name}' successfully configured on GitHub.")
        else:
            print(f"  ✗ Failed to upload '{secret_name}', status: {resp.status}")

def main():
    token = get_github_token()
    if not token:
        print("Error: GitHub token not found in ~/.git-credentials")
        sys.exit(1)

    repo = "Myselfnandha/helium-android"
    os.makedirs("keystore", exist_ok=True)
    keystore_path = os.path.abspath("keystore/helium-release.jks")
    alias = "helium"

    # Step 1: Generate Keystore if not exists
    if not os.path.exists(keystore_path):
        print(f"Generating new Android release keystore: {keystore_path}...")
        password = generate_random_password(24)
        cmd = [
            "keytool", "-genkeypair",
            "-v",
            "-keystore", keystore_path,
            "-alias", alias,
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-storepass", password,
            "-keypass", password,
            "-dname", "CN=Helium Browser, OU=Mobile, O=Helium, C=US"
        ]
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        if res.returncode != 0:
            print("Error generating keystore:", res.stderr)
            sys.exit(1)
        print("✓ Keystore generated successfully.")

        # Save credentials locally (ignored by git)
        with open("keystore/credentials.env", "w") as f:
            f.write(f"KEY_ALIAS={alias}\n")
            f.write(f"KEY_PASSWORD={password}\n")
            f.write(f"STORE_PASSWORD={password}\n")
            f.write(f"KEYSTORE_PATH={keystore_path}\n")
        os.chmod("keystore/credentials.env", 0o600)
    else:
        print(f"Using existing keystore: {keystore_path}")
        with open("keystore/credentials.env") as f:
            lines = f.readlines()
        data = dict(line.strip().split("=", 1) for line in lines if "=" in line)
        password = data["KEY_PASSWORD"]

    # Step 2: Fetch GitHub Public Key for Actions Secrets
    print(f"\nFetching repository public key from GitHub ({repo})...")
    pk_req = urllib.request.Request(
        f"https://api.github.com/repos/{repo}/actions/secrets/public-key",
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github.v3+json",
            "User-Agent": "Helium-KeySetup"
        }
    )
    with urllib.request.urlopen(pk_req) as resp:
        pk_data = json.loads(resp.read().decode())
        public_key_b64 = pk_data["key"]
        key_id = pk_data["key_id"]

    # Step 3: Base64 encode the keystore file
    with open(keystore_path, "rb") as f:
        keystore_b64 = base64.b64encode(f.read()).decode("utf-8")

    # Step 4: Encrypt and upload secrets to GitHub
    print("\nEncrypting and uploading GitHub Actions repository secrets...")
    secrets_map = {
        "SIGNING_KEYSTORE": keystore_b64,
        "KEY_ALIAS": alias,
        "KEY_PASSWORD": password,
        "STORE_PASSWORD": password
    }

    for name, val in secrets_map.items():
        encrypted = encrypt_secret(public_key_b64, val)
        upload_secret(repo, token, name, encrypted, key_id)

    print("\n✓ All release signing secrets are now active in GitHub Actions!")

if __name__ == "__main__":
    main()
