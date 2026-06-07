"""Asignación inicial de acceso para usuarios nuevos."""

from sqlalchemy.orm import Session

from db.models import MembresiaViaje, RolViaje, Usuario, Viaje


def ensure_default_membresia(db: Session, usuario: Usuario) -> bool:
    """
    Si el usuario no tiene ningún viaje, lo vincula al viaje por defecto (id=1)
    como ORGANIZADOR — mismo comportamiento que los endpoints legacy.
    Retorna True si se creó una membresía nueva.
    """
    tiene = (
        db.query(MembresiaViaje)
        .filter(MembresiaViaje.usuario_id == usuario.id)
        .count()
    )
    if tiene:
        return False

    viaje = (
        db.query(Viaje).filter(Viaje.id == 1).first()
        or db.query(Viaje).order_by(Viaje.id).first()
    )
    if not viaje:
        return False

    db.add(
        MembresiaViaje(
            viaje_id=viaje.id,
            usuario_id=usuario.id,
            familia_id=None,
            rol=RolViaje.ORGANIZADOR,
        )
    )
    db.commit()
    return True