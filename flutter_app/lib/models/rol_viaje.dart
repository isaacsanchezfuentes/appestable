enum RolViaje {
  organizador,
  jefeFamilia,
  miembro;

  static RolViaje fromApi(String? value) {
    switch (value?.toUpperCase()) {
      case 'ORGANIZADOR':
        return RolViaje.organizador;
      case 'JEFE_FAMILIA':
        return RolViaje.jefeFamilia;
      default:
        return RolViaje.miembro;
    }
  }

  String get apiValue {
    switch (this) {
      case RolViaje.organizador:
        return 'ORGANIZADOR';
      case RolViaje.jefeFamilia:
        return 'JEFE_FAMILIA';
      case RolViaje.miembro:
        return 'MIEMBRO';
    }
  }

  String get label {
    switch (this) {
      case RolViaje.organizador:
        return 'Organizador';
      case RolViaje.jefeFamilia:
        return 'Jefe de familia';
      case RolViaje.miembro:
        return 'Miembro';
    }
  }
}