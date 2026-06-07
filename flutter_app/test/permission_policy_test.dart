import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_app/domain/permission_policy.dart';
import 'package:flutter_app/domain/session_context.dart';
import 'package:flutter_app/models/persona.dart';
import 'package:flutter_app/models/rol_viaje.dart';

void main() {
  SessionContext session(RolViaje rol, {int? familiaId}) => SessionContext(
        viajeId: 1,
        viajeNombre: 'Test',
        rol: rol,
        familiaId: familiaId,
        isLoggedIn: true,
      );

  Persona persona(int familiaId, {RolViaje rol = RolViaje.miembro}) => Persona(
        id: 1,
        nombre: 'Ana',
        familiaId: familiaId,
        familiaNombre: 'Fam',
        esJefe: rol == RolViaje.jefeFamilia,
        rol: rol,
      );

  test('organizador ve todas las familias', () {
    final s = session(RolViaje.organizador);
    expect(PermissionPolicy.canViewFamilia(s, 1), isTrue);
    expect(PermissionPolicy.canViewFamilia(s, 99), isTrue);
  });

  test('jefe solo ve su familia', () {
    final s = session(RolViaje.jefeFamilia, familiaId: 2);
    expect(PermissionPolicy.canViewFamilia(s, 2), isTrue);
    expect(PermissionPolicy.canViewFamilia(s, 3), isFalse);
  });

  test('miembro no agrega personas', () {
    final s = session(RolViaje.miembro, familiaId: 1);
    expect(PermissionPolicy.canAddPersona(s, 1), isFalse);
  });

  test('jefe elimina solo miembros de su familia', () {
    final s = session(RolViaje.jefeFamilia, familiaId: 2);
    expect(PermissionPolicy.canDeletePersona(s, persona(2)), isTrue);
    expect(PermissionPolicy.canDeletePersona(s, persona(3)), isFalse);
    expect(
      PermissionPolicy.canDeletePersona(s, persona(2, rol: RolViaje.jefeFamilia)),
      isFalse,
    );
  });

  test('miembro no crea actividades', () {
    expect(PermissionPolicy.canCreateActividad(session(RolViaje.miembro, familiaId: 1)), isFalse);
  });

  test('organizador ve resumen global', () {
    expect(PermissionPolicy.canViewResumenGlobal(session(RolViaje.organizador)), isTrue);
    expect(PermissionPolicy.canViewResumenGlobal(session(RolViaje.miembro, familiaId: 1)), isFalse);
  });
}