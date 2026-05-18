from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, EmailStr
from jose import jwt
import requests
from sqlalchemy.orm import Session
from typing import Optional

# 🔥 BD IMPORTS (bien ubicados)
from db.base import Base
from db.session import engine, SessionLocal
from db import models # Importar modelos para que Base los registre

app = FastAPI()
security = HTTPBearer()

# 🔥 Dependencia para la base de datos
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# 🔥 MODELOS DE VALIDACIÓN (Pydantic)
class PersonaCreate(BaseModel):
    nombre: str
    email: Optional[EmailStr] = None
    celular: Optional[str] = None
    familia_nombre: str
    es_jefe: bool = False

# ... (resto del código de AUTH0_CONFIG y JWKS se mantiene igual)

# 🔥 AUTH0 CONFIG
AUTH0_DOMAIN = "dev-zbne73xs48twrr2a.us.auth0.com"
ALGORITHMS = ["RS256"]
API_AUDIENCE = "https://appestable-api"

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


# 🔥 GUARDAR USUARIO (Endpoint Real)
@app.post("/persona", status_code=status.HTTP_201_CREATED)
def crear_persona(
    persona_data: PersonaCreate, 
    db: Session = Depends(get_db),
    token_data: dict = Depends(verify_token)
):
    # El auth0_id viene en el campo 'sub' del token
    auth0_id = token_data.get("sub")
    
    # 1. Manejar la familia
    familia = db.query(models.Familia).filter(models.Familia.nombre_familia == persona_data.familia_nombre).first()
    if not familia:
        familia = models.Familia(nombre_familia=persona_data.familia_nombre)
        db.add(familia)
        db.commit()
        db.refresh(familia)

    # 2. Verificar si la persona ya existe
    db_persona = db.query(models.Persona).filter(models.Persona.auth0_id == auth0_id).first()
    if db_persona:
        # Si ya existe, podríamos actualizar datos si quisiéramos
        return {"status": "ok", "message": "El usuario ya estaba registrado", "id": db_persona.id}

    # 3. Crear nueva persona
    nueva_persona = models.Persona(
        nombre=persona_data.nombre,
        email=persona_data.email or token_data.get("email"),
        celular=persona_data.celular,
        es_jefe=persona_data.es_jefe,
        auth0_id=auth0_id,
        familia_id=familia.id
    )
    
    try:
        db.add(nueva_persona)
        db.commit()
        db.refresh(nueva_persona)
        return {"status": "success", "id": nueva_persona.id}
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Error al guardar: {str(e)}")

# 🔥 GUARDAR USUARIO (placeholder anterior - lo podemos quitar o dejar)
@app.post("/guardar_usuario")
def guardar_usuario(usuario: Usuario):
    print("Usuario recibido:", usuario.email)

    return {
        "status": "ok",
        "email": usuario.email
    }