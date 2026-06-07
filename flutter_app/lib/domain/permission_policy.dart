import '../models/persona.dart';
import '../models/rol_viaje.dart';
import 'session_context.dart';

class PermissionPolicy {
  static bool canViewFamilia(SessionContext session, int familiaId) {
    switch (session.rol) {
      case RolViaje.organizador:
        return true;
      case RolViaje.jefeFamilia:
      case RolViaje.miembro:
        return session.familiaId == familiaId;
    }
  }

  static bool canCreateViaje(SessionContext session) =>
      session.rol == RolViaje.organizador;

  static bool canAddPersona(SessionContext session, int familiaId) {
    switch (session.rol) {
      case RolViaje.organizador:
        return true;
      case RolViaje.jefeFamilia:
        return session.familiaId == familiaId;
      case RolViaje.miembro:
        return false;
    }
  }

  static bool canDeletePersona(SessionContext session, Persona persona) {
    switch (session.rol) {
      case RolViaje.organizador:
        return persona.rol != RolViaje.organizador;
      case RolViaje.jefeFamilia:
        return session.familiaId == persona.familiaId &&
            persona.rol == RolViaje.miembro;
      case RolViaje.miembro:
        return false;
    }
  }

  static bool canCreateActividad(SessionContext session) {
    switch (session.rol) {
      case RolViaje.organizador:
      case RolViaje.jefeFamilia:
        return true;
      case RolViaje.miembro:
        return false;
    }
  }

  static bool canDeleteActividad(SessionContext session) {
    switch (session.rol) {
      case RolViaje.organizador:
      case RolViaje.jefeFamilia:
        return true;
      case RolViaje.miembro:
        return false;
    }
  }

  static bool canSelectParticipante(SessionContext session, Persona persona) {
    switch (session.rol) {
      case RolViaje.organizador:
        return true;
      case RolViaje.jefeFamilia:
        return session.familiaId == persona.familiaId;
      case RolViaje.miembro:
        return false;
    }
  }

  static bool canEditParticipacion(SessionContext session, int familiaId) {
    switch (session.rol) {
      case RolViaje.organizador:
        return true;
      case RolViaje.jefeFamilia:
        return session.familiaId == familiaId;
      case RolViaje.miembro:
        return false;
    }
  }

  static bool canViewResumenGlobal(SessionContext session) =>
      session.rol == RolViaje.organizador;
}