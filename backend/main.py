from fastapi import FastAPI, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
from jose import jwt
import requests

# 🔥 BD IMPORTS (bien ubicados)
from db.base import Base
from db.session import engine

app = FastAPI()
security = HTTPBearer()

# 🔥 AUTH0 CONFIG (Configurar via variables de entorno o archivo .env)
AUTH0_DOMAIN = "YOUR_AUTH0_DOMAIN"
ALGORITHMS = ["RS256"]
API_AUDIENCE = "YOUR_API_AUDIENCE"

# 🔥 MODELO SIMPLE (request)
class Usuario(BaseModel):
    email: str


# 🔥 CREATE TABLES (IMPORTANTE: solo al iniciar app)
@app.on_event("startup")
def startup():
    Base.metadata.create_all(bind=engine)


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
        raise HTTPException(status_code=401, detail="Invalid token header")

    try:
        payload = jwt.decode(
            token,
            rsa_key,
            algorithms=ALGORITHMS,
            audience=API_AUDIENCE, # <-- Añadimos validación de audiencia
            issuer=f"https://{AUTH0_DOMAIN}/"
        )
        return payload

    except Exception as e:
        print("JWT ERROR:", str(e))
        raise HTTPException(status_code=401, detail="Token inválido")


# 🔥 ROOT
@app.get("/")
def root():
    return {
        "status": "ok",
        "message": "Backend activo 🚀"
    }


# 🔥 VALIDAR USUARIO (Auth0)
@app.get("/me")
def me(user=Depends(verify_token)):
    return {
        "user_id": user.get("sub"),
        "email": user.get("email"),
        "message": "Usuario validado correctamente"
    }


# 🔥 GUARDAR USUARIO (placeholder)
@app.post("/guardar_usuario")
def guardar_usuario(usuario: Usuario):
    print("Usuario recibido:", usuario.email)

    return {
        "status": "ok",
        "email": usuario.email
    }