"""Tests de asignación automática al viaje por defecto."""

from db.models import MembresiaViaje, RolViaje, Usuario, Viaje
from services.bootstrap import ensure_default_membresia


def test_crea_membresia_organizador_en_viaje_1(db):
    viaje = Viaje(id=1, nombre="Viaje Principal")
    db.add(viaje)
    db.commit()

    usuario = Usuario(auth0_id="auth0|new", email="new@test.com", nombre="Nuevo")
    db.add(usuario)
    db.commit()

    created = ensure_default_membresia(db, usuario)
    assert created is True

    membresia = (
        db.query(MembresiaViaje)
        .filter(MembresiaViaje.usuario_id == usuario.id)
        .first()
    )
    assert membresia is not None
    assert membresia.viaje_id == 1
    assert membresia.rol == RolViaje.ORGANIZADOR


def test_no_duplica_si_ya_tiene_membresia(db):
    viaje = Viaje(id=1, nombre="Viaje Principal")
    usuario = Usuario(auth0_id="auth0|x", email="x@test.com", nombre="X")
    db.add_all([viaje, usuario])
    db.commit()

    db.add(
        MembresiaViaje(
            viaje_id=1,
            usuario_id=usuario.id,
            rol=RolViaje.MIEMBRO,
            familia_id=None,
        )
    )
    db.commit()

    assert ensure_default_membresia(db, usuario) is False
    assert db.query(MembresiaViaje).filter(MembresiaViaje.usuario_id == usuario.id).count() == 1