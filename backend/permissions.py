from typing import Iterable, Optional

from fastapi import Depends, HTTPException
from sqlalchemy.orm import Session

from auth import get_or_create_usuario
from db.models import MembresiaViaje, Persona, RolViaje, Usuario
from db.session import get_db


class ViajeAccess:
    def __init__(self, membresia: MembresiaViaje, usuario: Usuario):
        self.membresia = membresia
        self.usuario = usuario
        self.rol = membresia.rol
        self.familia_id = membresia.familia_id
        self.viaje_id = membresia.viaje_id


def get_membresia(
    db: Session,
    viaje_id: int,
    usuario_id: int,
) -> Optional[MembresiaViaje]:
    return (
        db.query(MembresiaViaje)
        .filter(
            MembresiaViaje.viaje_id == viaje_id,
            MembresiaViaje.usuario_id == usuario_id,
        )
        .first()
    )


def require_viaje_access(
    allowed_roles: Optional[Iterable[RolViaje]] = None,
):
    def dependency(
        viaje_id: int,
        usuario: Usuario = Depends(get_or_create_usuario),
        db: Session = Depends(get_db),
    ) -> ViajeAccess:
        membresia = get_membresia(db, viaje_id, usuario.id)
        if not membresia:
            raise HTTPException(status_code=403, detail="No tienes acceso a este viaje")
        if allowed_roles and membresia.rol not in allowed_roles:
            raise HTTPException(status_code=403, detail="Permiso denegado para esta operación")
        return ViajeAccess(membresia=membresia, usuario=usuario)

    return dependency


def can_view_familia(access: ViajeAccess, familia_id: int) -> bool:
    if access.rol == RolViaje.ORGANIZADOR:
        return True
    return access.familia_id == familia_id


def can_add_persona(access: ViajeAccess, familia_id: int) -> bool:
    if access.rol == RolViaje.ORGANIZADOR:
        return True
    if access.rol == RolViaje.JEFE_FAMILIA:
        return access.familia_id == familia_id
    return False


def can_delete_persona(access: ViajeAccess, persona: Persona) -> bool:
    if persona.rol == RolViaje.ORGANIZADOR:
        return False
    if access.rol == RolViaje.ORGANIZADOR:
        return True
    if access.rol == RolViaje.JEFE_FAMILIA:
        return access.familia_id == persona.familia_id and persona.rol == RolViaje.MIEMBRO
    return False


def can_create_actividad(access: ViajeAccess) -> bool:
    return access.rol in (RolViaje.ORGANIZADOR, RolViaje.JEFE_FAMILIA)


def can_edit_participacion(access: ViajeAccess, familia_id: int) -> bool:
    if access.rol == RolViaje.ORGANIZADOR:
        return True
    if access.rol == RolViaje.JEFE_FAMILIA:
        return access.familia_id == familia_id
    return False


def can_select_participante(access: ViajeAccess, persona: Persona) -> bool:
    if access.rol == RolViaje.ORGANIZADOR:
        return True
    if access.rol == RolViaje.JEFE_FAMILIA:
        return access.familia_id == persona.familia_id
    return False