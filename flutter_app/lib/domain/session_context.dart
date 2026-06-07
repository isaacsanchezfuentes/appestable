import '../models/rol_viaje.dart';

class SessionContext {
  final int viajeId;
  final String viajeNombre;
  final RolViaje rol;
  final int? familiaId;
  final int? usuarioId;
  final bool isLoggedIn;

  const SessionContext({
    required this.viajeId,
    required this.viajeNombre,
    required this.rol,
    this.familiaId,
    this.usuarioId,
    this.isLoggedIn = false,
  });

  static const SessionContext empty = SessionContext(
    viajeId: 0,
    viajeNombre: '',
    rol: RolViaje.miembro,
    isLoggedIn: false,
  );
}