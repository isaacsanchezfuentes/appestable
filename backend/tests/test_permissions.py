"""Tests unitarios de la matriz de permisos."""

from db.models import MembresiaViaje, Persona, RolViaje, Usuario
from permissions import (
    ViajeAccess,
    can_add_persona,
    can_create_actividad,
    can_delete_persona,
    can_edit_participacion,
    can_select_participante,
    can_view_familia,
)


def _access(rol: RolViaje, familia_id: int | None = None, viaje_id: int = 1) -> ViajeAccess:
    usuario = Usuario(id=1, auth0_id="auth0|1", email="u@test.com", nombre="U")
    membresia = MembresiaViaje(
        id=1,
        viaje_id=viaje_id,
        usuario_id=1,
        familia_id=familia_id,
        rol=rol,
    )
    return ViajeAccess(membresia=membresia, usuario=usuario)


def _persona(familia_id: int, rol: RolViaje = RolViaje.MIEMBRO) -> Persona:
    return Persona(
        id=10,
        nombre="Ana",
        email="ana@test.com",
        celular="",
        es_jefe=rol == RolViaje.JEFE_FAMILIA,
        familia_id=familia_id,
        viaje_id=1,
        rol=rol,
    )


class TestCanViewFamilia:
    def test_organizador_ve_todas(self):
        access = _access(RolViaje.ORGANIZADOR)
        assert can_view_familia(access, 1) is True
        assert can_view_familia(access, 99) is True

    def test_jefe_solo_su_familia(self):
        access = _access(RolViaje.JEFE_FAMILIA, familia_id=2)
        assert can_view_familia(access, 2) is True
        assert can_view_familia(access, 3) is False

    def test_miembro_solo_su_familia(self):
        access = _access(RolViaje.MIEMBRO, familia_id=5)
        assert can_view_familia(access, 5) is True
        assert can_view_familia(access, 1) is False


class TestCanAddPersona:
    def test_organizador_siempre(self):
        assert can_add_persona(_access(RolViaje.ORGANIZADOR), 1) is True

    def test_jefe_solo_su_familia(self):
        access = _access(RolViaje.JEFE_FAMILIA, familia_id=2)
        assert can_add_persona(access, 2) is True
        assert can_add_persona(access, 3) is False

    def test_miembro_nunca(self):
        assert can_add_persona(_access(RolViaje.MIEMBRO, familia_id=1), 1) is False


class TestCanDeletePersona:
    def test_nadie_borra_organizador_persona(self):
        org_persona = _persona(1, RolViaje.ORGANIZADOR)
        assert can_delete_persona(_access(RolViaje.ORGANIZADOR), org_persona) is False

    def test_organizador_borra_miembros(self):
        assert can_delete_persona(_access(RolViaje.ORGANIZADOR), _persona(1)) is True

    def test_jefe_solo_miembros_de_su_familia(self):
        access = _access(RolViaje.JEFE_FAMILIA, familia_id=2)
        assert can_delete_persona(access, _persona(2)) is True
        assert can_delete_persona(access, _persona(3)) is False
        assert can_delete_persona(access, _persona(2, RolViaje.JEFE_FAMILIA)) is False


class TestCanCreateActividad:
    def test_organizador_y_jefe(self):
        assert can_create_actividad(_access(RolViaje.ORGANIZADOR)) is True
        assert can_create_actividad(_access(RolViaje.JEFE_FAMILIA, 1)) is True

    def test_miembro_no(self):
        assert can_create_actividad(_access(RolViaje.MIEMBRO, 1)) is False


class TestCanEditParticipacion:
    def test_jefe_solo_su_familia(self):
        access = _access(RolViaje.JEFE_FAMILIA, familia_id=4)
        assert can_edit_participacion(access, 4) is True
        assert can_edit_participacion(access, 1) is False

    def test_miembro_no_edita(self):
        assert can_edit_participacion(_access(RolViaje.MIEMBRO, 1), 1) is False


class TestCanSelectParticipante:
    def test_jefe_solo_su_familia(self):
        access = _access(RolViaje.JEFE_FAMILIA, familia_id=2)
        assert can_select_participante(access, _persona(2)) is True
        assert can_select_participante(access, _persona(3)) is False