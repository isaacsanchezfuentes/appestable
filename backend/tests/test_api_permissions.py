"""Tests de integración HTTP con permisos por rol."""

from db.models import (
    Actividad,
    Familia,
    MembresiaViaje,
    Participacion,
    Persona,
    RolViaje,
    Usuario,
    Viaje,
)


def _seed_viaje(db, viaje_id: int = 1):
    viaje = Viaje(id=viaje_id, nombre="Viaje Test")
    db.add(viaje)
    db.commit()


def _seed_usuario(db, auth0_id: str = "test|local-user") -> Usuario:
    usuario = db.query(Usuario).filter(Usuario.auth0_id == auth0_id).first()
    if usuario:
        return usuario
    usuario = Usuario(auth0_id=auth0_id, email="test@appestable.local", nombre="Test User")
    db.add(usuario)
    db.commit()
    db.refresh(usuario)
    return usuario


def _membresia(db, usuario: Usuario, rol: RolViaje, familia_id: int | None = None):
    m = MembresiaViaje(
        viaje_id=1,
        usuario_id=usuario.id,
        familia_id=familia_id,
        rol=rol,
    )
    db.add(m)
    db.commit()


def _familia(db, nombre: str, viaje_id: int = 1) -> Familia:
    f = Familia(nombre_familia=nombre, viaje_id=viaje_id)
    db.add(f)
    db.commit()
    db.refresh(f)
    return f


def _persona(db, nombre: str, familia: Familia, rol: RolViaje = RolViaje.MIEMBRO) -> Persona:
    p = Persona(
        nombre=nombre,
        email=f"{nombre.lower()}@test.com",
        celular="",
        es_jefe=rol == RolViaje.JEFE_FAMILIA,
        familia_id=familia.id,
        viaje_id=familia.viaje_id,
        rol=rol,
    )
    db.add(p)
    db.commit()
    db.refresh(p)
    return p


class TestApiPermissions:
    def test_sin_membresia_403(self, client, db):
        _seed_viaje(db)
        _seed_usuario(db)
        res = client.get("/viajes/1/personas")
        assert res.status_code == 403

    def test_miembro_no_puede_crear_persona(self, client, db):
        _seed_viaje(db)
        usuario = _seed_usuario(db)
        fam = _familia(db, "García")
        _membresia(db, usuario, RolViaje.MIEMBRO, familia_id=fam.id)

        res = client.post(
            "/viajes/1/personas",
            json={"nombre": "Nuevo", "familia_nombre": "García", "es_jefe": False},
        )
        assert res.status_code == 403

    def test_jefe_puede_crear_en_su_familia(self, client, db):
        _seed_viaje(db)
        usuario = _seed_usuario(db)
        fam = _familia(db, "López")
        _membresia(db, usuario, RolViaje.JEFE_FAMILIA, familia_id=fam.id)

        res = client.post(
            "/viajes/1/personas",
            json={"nombre": "Pedro", "familia_nombre": "López", "es_jefe": False},
        )
        assert res.status_code == 201

    def test_jefe_no_puede_crear_en_otra_familia(self, client, db):
        _seed_viaje(db)
        usuario = _seed_usuario(db)
        fam_propia = _familia(db, "Propia")
        _familia(db, "Ajena")
        _membresia(db, usuario, RolViaje.JEFE_FAMILIA, familia_id=fam_propia.id)

        res = client.post(
            "/viajes/1/personas",
            json={"nombre": "Intruso", "familia_nombre": "Ajena", "es_jefe": False},
        )
        assert res.status_code == 403

    def test_miembro_no_puede_crear_actividad(self, client, db):
        _seed_viaje(db)
        usuario = _seed_usuario(db)
        fam = _familia(db, "Ruiz")
        persona = _persona(db, "Luis", fam)
        _membresia(db, usuario, RolViaje.MIEMBRO, familia_id=fam.id)

        res = client.post(
            "/viajes/1/actividades",
            json={
                "nombre": "Cena",
                "fecha": "2026-06-01",
                "costo_total": 100.0,
                "participantes_ids": [persona.id],
            },
        )
        assert res.status_code == 403

    def test_miembro_no_puede_editar_participacion(self, client, db):
        _seed_viaje(db)
        usuario = _seed_usuario(db)
        fam = _familia(db, "Pérez")
        persona = _persona(db, "María", fam)
        _membresia(db, usuario, RolViaje.MIEMBRO, familia_id=fam.id)

        act = Actividad(nombre="Hotel", fecha="2026-06-01", costo_total=200, viaje_id=1)
        db.add(act)
        db.commit()
        db.refresh(act)

        part = Participacion(persona_id=persona.id, actividad_id=act.id, costo_individual=100)
        db.add(part)
        db.commit()
        db.refresh(part)

        res = client.put(f"/viajes/1/participaciones/{part.id}", json={"pagado": True})
        assert res.status_code == 403

    def test_resumen_global_solo_organizador(self, client, db):
        _seed_viaje(db)
        usuario = _seed_usuario(db)
        fam = _familia(db, "Test")
        _persona(db, "A", fam)
        _membresia(db, usuario, RolViaje.MIEMBRO, familia_id=fam.id)

        res = client.get("/viajes/1/resumen")
        assert res.status_code == 200
        assert res.json()["global"] is None

        db.query(MembresiaViaje).delete()
        _membresia(db, usuario, RolViaje.ORGANIZADOR)
        res = client.get("/viajes/1/resumen")
        assert res.status_code == 200
        assert res.json()["global"] is not None