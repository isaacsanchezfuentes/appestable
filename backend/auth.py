import logging
from typing import Optional

import requests
from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import jwt
from sqlalchemy.orm import Session

from config import get_settings
from db.models import Usuario
from db.session import get_db

logger = logging.getLogger(__name__)
security = HTTPBearer(auto_error=False)
settings = get_settings()


def verify_token(credentials: Optional[HTTPAuthorizationCredentials] = Depends(security)) -> dict:
    if settings.disable_auth:
        return {
            "sub": "test|local-user",
            "email": "test@appestable.local",
            "name": "Test User",
        }

    if credentials is None or not credentials.credentials:
        raise HTTPException(status_code=401, detail="Token no proporcionado")

    token = credentials.credentials
    try:
        url = f"https://{settings.auth0_domain}/.well-known/jwks.json"
        jwks = requests.get(url, timeout=10).json()
        header = jwt.get_unverified_header(token)
        rsa_key = next(key for key in jwks["keys"] if key["kid"] == header["kid"])
        return jwt.decode(
            token,
            rsa_key,
            algorithms=["RS256"],
            audience=settings.auth0_audience,
            issuer=settings.auth0_issuer,
        )
    except Exception as exc:
        logger.error("Error de token: %s", exc)
        raise HTTPException(status_code=401, detail=f"Token inválido: {exc}") from exc


def _claim_email(token_data: dict) -> str:
    return token_data.get("email") or token_data.get("https://appestable/email") or "usuario@appestable.local"


def _claim_name(token_data: dict, email: str) -> str:
    return token_data.get("name") or token_data.get("nickname") or email.split("@")[0]


def get_or_create_usuario(
    token_data: dict = Depends(verify_token),
    db: Session = Depends(get_db),
) -> Usuario:
    auth0_id = token_data.get("sub")
    if not auth0_id:
        raise HTTPException(status_code=401, detail="Token sin identificador de usuario")

    email = _claim_email(token_data)
    nombre = _claim_name(token_data, email)

    usuario = db.query(Usuario).filter(Usuario.auth0_id == auth0_id).first()
    if usuario:
        if usuario.email != email or usuario.nombre != nombre:
            usuario.email = email
            usuario.nombre = nombre
            db.commit()
            db.refresh(usuario)
        return usuario

    usuario = Usuario(auth0_id=auth0_id, email=email, nombre=nombre)
    db.add(usuario)
    db.commit()
    db.refresh(usuario)
    return usuario


def is_global_admin(token_data: dict) -> bool:
    roles = (
        token_data.get("https://appestable/role")
        or token_data.get("role")
        or token_data.get("roles")
        or []
    )
    if isinstance(roles, str):
        roles = [roles]
    return "admin" in [str(r).lower() for r in roles]


def require_global_admin(
    token_data: dict = Depends(verify_token),
) -> dict:
    if not is_global_admin(token_data):
        raise HTTPException(status_code=403, detail="Permiso denegado: Se requiere rol de Admin")
    return token_data