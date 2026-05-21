import 'package:flutter/material.dart';
import 'package:auth0_flutter/auth0_flutter.dart';
import '../core/api_config.dart';

class AuthService with ChangeNotifier {
  final Auth0 auth0 = Auth0(ApiConfig.auth0Domain, ApiConfig.auth0ClientId);
  Credentials? _credentials;

  Future<bool> login() async {
    try {
      _credentials = await auth0
          .webAuthentication(scheme: "appestable")
          .login(
            audience: "https://appestable-api",
            scopes: {'openid', 'profile', 'email'},
          );
      notifyListeners();
      return _credentials != null;
    } catch (e) {
      print("Error en Login Flutter: $e");
      return false;
    }
  }

  Future<void> logout() async {
    await auth0.webAuthentication(scheme: "appestable").logout();
    _credentials = null;
    notifyListeners();
  }

  String? get accessToken => _credentials?.accessToken;
  String? get userEmail => _credentials?.user.email;
  bool get isLoggedIn => _credentials != null;

  bool get isAdmin {
    if (_credentials == null) return false;
    
    // Matriz de claims según documentación: role, roles, o namespace
    final Map<String, dynamic>? claims = _credentials?.user.customClaims;
    if (claims == null) return false;

    final dynamic roleField = claims['role'] ?? 
                             claims['roles'] ?? 
                             claims['https://appestable/role'];

    if (roleField is String) {
      return roleField.toLowerCase() == 'admin';
    } else if (roleField is List) {
      return roleField.any((r) => r.toString().toLowerCase() == 'admin');
    }
    
    return false;
  }
}
