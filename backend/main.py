from fastapi import FastAPI, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt
import requests

app = FastAPI()
security = HTTPBearer()

AUTH0_DOMAIN = "dev-zbne73xs48twrr2a.us.auth0.com"
AUDIENCE = "https://appestable-api"
ALGORITHMS = ["RS256"]

def get_jwks():
    url = f"https://{AUTH0_DOMAIN}/.well-known/jwks.json"
    return requests.get(url).json()

def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials

    jwks = get_jwks()
    unverified_header = jwt.get_unverified_header(token)

    rsa_key = {}

    for key in jwks["keys"]:
        if key["kid"] == unverified_header["kid"]:
            rsa_key = {
                "kty": key["kty"],
                "kid": key["kid"],
                "use": key["use"],
                "n": key["n"],
                "e": key["e"]
            }

    if not rsa_key:
        raise HTTPException(status_code=401, detail="Invalid token header")

    try:
        payload = jwt.decode(
            token,
            rsa_key,
            algorithms=ALGORITHMS,
            audience=AUDIENCE,
            issuer=f"https://{AUTH0_DOMAIN}/"
        )

        return payload

    except Exception as e:
        raise HTTPException(status_code=401, detail="Token inválido")


@app.get("/")
def root():
    return {"status": "ok", "message": "Backend activo 🚀"}


@app.get("/me")
def me(user=Depends(verify_token)):
    return {
        "user_id": user["sub"],
        "email": user.get("email"),
        "message": "Usuario validado correctamente"
    }