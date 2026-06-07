/// Configuración centralizada.
///
/// URL según dónde corres la app:
/// - Celular físico (misma WiFi): http://TU_IP_LAN:8000  (ej. 192.168.0.4)
/// - Emulador Android: http://10.0.2.2:8000
/// - Flutter en Windows: http://127.0.0.1:8000
///
/// Sobrescribir: `flutter run --dart-define-from-file=dart_defines.json`
class ApiConfig {
  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://192.168.0.3:8000',
  );

  static const String auth0Domain = String.fromEnvironment(
    'AUTH0_DOMAIN',
    defaultValue: 'dev-zbne73xs48twrr2a.us.auth0.com',
  );

  static const String auth0ClientId = String.fromEnvironment(
    'AUTH0_CLIENT_ID',
    defaultValue: 'q9hPzu6loAkYwN0oNi6bakQi3T3t0iA4',
  );

  static const String auth0Audience = String.fromEnvironment(
    'AUTH0_AUDIENCE',
    defaultValue: 'https://appestable-api',
  );
}