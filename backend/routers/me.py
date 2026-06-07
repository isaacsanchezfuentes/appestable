from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from auth import get_or_create_usuario
from db.models import MembresiaViaje, Usuario, Viaje
from db.session import get_db
from schemas import MeResponse, MembresiaOut, UsuarioOut
from services.bootstrap import ensure_default_membresia

router = APIRouter(tags=["auth"])


@router.get("/me", response_model=MeResponse)
def get_me(
    usuario: Usuario = Depends(get_or_create_usuario),
    db: Session = Depends(get_db),
):
    ensure_default_membresia(db, usuario)

    membresias = (
        db.query(MembresiaViaje)
        .filter(MembresiaViaje.usuario_id == usuario.id)
        .all()
    )

    membresias_out = []
    for membresia in membresias:
        viaje = db.query(Viaje).filter(Viaje.id == membresia.viaje_id).first()
        familia_nombre = None
        if membresia.familia_id and membresia.familia:
            familia_nombre = membresia.familia.nombre_familia
        membresias_out.append(
            MembresiaOut(
                viaje_id=membresia.viaje_id,
                viaje_nombre=viaje.nombre if viaje else "",
                rol=membresia.rol.value,
                familia_id=membresia.familia_id,
                familia_nombre=familia_nombre,
            )
        )

    return MeResponse(
        usuario=UsuarioOut(
            id=usuario.id,
            email=usuario.email,
            nombre=usuario.nombre,
            auth0_id=usuario.auth0_id,
        ),
        membresias=membresias_out,
    )