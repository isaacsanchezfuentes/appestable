from fastapi import FastAPI, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

from pydantic import BaseModel

from jose import jwt

import requests

app = FastAPI()

security = HTTPBearer()

# 🔥 AUTH0 CONFIG

AUTH0_DOMAIN = "dev-zbne73xs48twrr2a.us.auth0.com"

# ⚠️ TEMPORALMENTE NO USAMOS AUDIENCE
# porque quitaste .withAudience() del Android login

ALGORITHMS = ["RS256"]


# 🔥 MODELO

class Usuario(BaseModel):
    email: str


# 🔥 JWKS

def get_jwks():

    url = f"https://{AUTH0_DOMAIN}/.well-known/jwks.json"

    return requests.get(url).json()


# 🔥 VERIFY TOKEN

def verify_token(
    credentials: HTTPAuthorizationCredentials = Depends(security)
):

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

        raise HTTPException(
            status_code=401,
            detail="Invalid token header"
        )

    try:

        # 🔥 VALIDACIÓN JWT

        payload = jwt.decode(
            token,
            rsa_key,
            algorithms=ALGORITHMS,
            issuer=f"https://{AUTH0_DOMAIN}/"
        )

        return payload

    except Exception as e:

        print("JWT ERROR:", str(e))

        raise HTTPException(
            status_code=401,
            detail="Token inválido"
        )


# 🔥 ROOT

@app.get("/")
def root():

    return {
        "status": "ok",
        "message": "Backend activo 🚀"
    }


# 🔥 VALIDAR USUARIO

@app.get("/me")
def me(user=Depends(verify_token)):

    return {
        "user_id": user.get("sub"),
        "email": user.get("email"),
        "message": "Usuario validado correctamente"
    }


# 🔥 GUARDAR USUARIO

@app.post("/guardar_usuario")
def guardar_usuario(
    usuario: Usuario
):

    print("Usuario recibido:", usuario.email)

    return {
        "status": "ok",
        "email": usuario.email
    }