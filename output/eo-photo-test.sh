export LANG=C LC_ALL=C
cd /opt/earth-online/deploy || exit 1

SECRET=$(grep '^JWT_SECRET=' /opt/earth-online/.env | cut -d= -f2-)
ROW=$(docker compose --env-file ../.env exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 -N -B earth_online -e "SELECT CONCAT(r.id, \"|\", u.username, \"|\", u.id) FROM records r JOIN users u ON u.id = r.user_id ORDER BY r.id DESC LIMIT 1;"' 2>/dev/null)
RECID=$(echo "$ROW" | cut -d'|' -f1)
USERNAME=$(echo "$ROW" | cut -d'|' -f2)
USERID=$(echo "$ROW" | cut -d'|' -f3)
echo "record id=${RECID}  owner=${USERNAME}  ownerId=${USERID}"

TOKEN=$(SECRET="$SECRET" U="$USERNAME" I="$USERID" python3 -c '
import base64, hmac, hashlib, json, os, time
b64 = lambda b: base64.urlsafe_b64encode(b).rstrip(b"=")
secret = os.environ["SECRET"].encode()
now = int(time.time())
head = {"alg": "HS512"}
body = {"sub": os.environ["U"], "userId": int(os.environ["I"]), "iat": now, "exp": now + 3600}
signing = b64(json.dumps(head, separators=(",", ":")).encode()) + b"." + b64(json.dumps(body, separators=(",", ":")).encode())
sig = b64(hmac.new(secret, signing, hashlib.sha512).digest())
print((signing + b"." + sig).decode())
')
echo "self-signed JWT: ${#TOKEN} chars"

curl -s -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/records/${RECID}" -o /tmp/rec.json
echo -n "api status: "; head -c 60 /tmp/rec.json; echo

python3 -c '
import json, re
d = json.load(open("/tmp/rec.json"))
url = None
for k, v in d.items():
    if isinstance(v, list):
        for x in v:
            if isinstance(x, str) and x.startswith("http"):
                url = x; break
    elif isinstance(v, str) and v.startswith("http"):
        url = v
    if url: break
print("photo field found:", bool(url))
if url:
    print("host+path:", url.split("?")[0])
    open("/tmp/photo_url.txt", "w").write(url)
'

if [ -s /tmp/photo_url.txt ]; then
  URL=$(cat /tmp/photo_url.txt)
  echo -n "1) via nginx public origin : "
  curl -s -o /dev/null -w "%{http_code} type=%{content_type} bytes=%{size_download}\n" "$URL"
  INNER=$(echo "$URL" | sed 's|http://190.92.245.191|http://127.0.0.1:9000|')
  echo -n "2) direct to minio (8080?) : "
  curl -s -o /dev/null -w "%{http_code}\n" --max-time 5 "$INNER"
  PATHONLY=$(echo "$URL" | sed 's|http://190.92.245.191||')
  echo -n "3) nginx -> minio with Host minio:9000 : "
  curl -s -o /dev/null -w "%{http_code} type=%{content_type}\n" --max-time 5 -H "Host: minio:9000" "http://127.0.0.1${PATHONLY}"
fi
echo "=== DONE ==="
