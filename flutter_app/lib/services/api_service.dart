import 'package:dio/dio.dart';
import '../core/api_config.dart';
import '../models/persona.dart';
import '../models/actividad.dart';
import '../models/participacion.dart';
import '../models/viaje.dart';
import '../models/me_response.dart';
import '../models/resumen.dart';

class ApiService {
  final Dio _dio = Dio(BaseOptions(
    baseUrl: ApiConfig.baseUrl,
    connectTimeout: const Duration(seconds: 15),
    receiveTimeout: const Duration(seconds: 15),
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    },
  ));

  Options _authOptions(String token) =>
      Options(headers: {'Authorization': "Bearer $token"});

  Future<MeResponse?> getMe(String token) async {
    try {
      final res = await _dio.get('/me', options: _authOptions(token));
      return MeResponse.fromJson(res.data as Map<String, dynamic>);
    } on DioException catch (e) {
      print("ApiService.getMe DioError: ${e.message}");
      return null;
    } catch (e) {
      print("ApiService.getMe Error: $e");
      return null;
    }
  }

  Future<List<Viaje>> getViajes(String token) async {
    try {
      final res = await _dio.get('/viajes', options: _authOptions(token));
      return (res.data as List)
          .map((v) => Viaje.fromJson(v as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      print("ApiService.getViajes DioError: ${e.message}");
      return [];
    } catch (e) {
      print("ApiService.getViajes Error: $e");
      return [];
    }
  }

  Future<String?> ensureMembership(String token, int viajeId) async {
    try {
      await _dio.post(
        "/viajes/$viajeId/ensure-membership",
        options: _authOptions(token),
      );
      return null;
    } on DioException catch (e) {
      return _handleDioError(e);
    } catch (e) {
      return e.toString();
    }
  }

  Future<({int? id, String? error})> crearViaje(
    String token,
    String nombre, {
    String descripcion = '',
  }) async {
    try {
      final res = await _dio.post(
        '/viajes',
        data: {'nombre': nombre, 'descripcion': descripcion},
        options: Options(
          headers: {'Authorization': "Bearer $token"},
          validateStatus: (status) => status != null && status < 500,
        ),
      );

      if (res.statusCode == 201 || res.statusCode == 200) {
        final data = res.data;
        if (data is Map && data['id'] != null) {
          return (id: (data['id'] as num).toInt(), error: null);
        }
        return (id: null, error: 'Respuesta inválida del servidor');
      }

      final detail = res.data is Map ? res.data['detail'] : res.data;
      return (id: null, error: detail?.toString() ?? 'Error ${res.statusCode}');
    } on DioException catch (e) {
      return (id: null, error: _connectionHint(e));
    } catch (e) {
      return (id: null, error: 'Error inesperado: $e');
    }
  }

  Future<ResumenViaje?> getResumen(String token, int viajeId) async {
    try {
      final res = await _dio.get(
        "/viajes/$viajeId/resumen",
        options: _authOptions(token),
      );
      return ResumenViaje.fromJson(res.data as Map<String, dynamic>);
    } on DioException catch (e) {
      print("ApiService.getResumen DioError: ${e.message}");
      return null;
    } catch (e) {
      print("ApiService.getResumen Error: $e");
      return null;
    }
  }

  Future<List<Persona>> getPersonas(String token, int viajeId) async {
    try {
      final res = await _dio.get(
        "/viajes/$viajeId/personas",
        options: _authOptions(token),
      );
      return (res.data as List)
          .map((p) => Persona.fromJson(p as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      print("ApiService.getPersonas DioError: ${e.message}");
      rethrow;
    } catch (e) {
      print("ApiService.getPersonas Error: $e");
      rethrow;
    }
  }

  Future<String?> registrarPersona(Persona p, String token, int viajeId) async {
    try {
      await _dio.post(
        "/viajes/$viajeId/personas",
        data: p.toJson(),
        options: _authOptions(token),
      );
      return null;
    } on DioException catch (e) {
      return _handleDioError(e);
    } catch (e) {
      return e.toString();
    }
  }

  Future<List<Actividad>> getActividades(String token, int viajeId) async {
    try {
      final res = await _dio.get(
        "/viajes/$viajeId/actividades",
        options: _authOptions(token),
      );
      return (res.data as List)
          .map((a) => Actividad.fromJson(a as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      print("ApiService.getActividades DioError: ${e.message}");
      rethrow;
    } catch (e) {
      print("ApiService.getActividades Error: $e");
      rethrow;
    }
  }

  Future<String?> registrarActividad(
    String nombre,
    double costo,
    String fecha,
    List<int> pIds,
    String token,
    int viajeId,
  ) async {
    try {
      await _dio.post(
        "/viajes/$viajeId/actividades",
        data: {
          'nombre': nombre,
          'costo_total': costo,
          'fecha': fecha,
          'participantes_ids': pIds,
        },
        options: _authOptions(token),
      );
      return null;
    } on DioException catch (e) {
      return _handleDioError(e);
    } catch (e) {
      return e.toString();
    }
  }

  Future<List<Participacion>> getParticipaciones(String token, int viajeId) async {
    try {
      final res = await _dio.get(
        "/viajes/$viajeId/participaciones",
        options: _authOptions(token),
      );
      return (res.data as List)
          .map((p) => Participacion.fromJson(p as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      print("ApiService.getParticipaciones DioError: ${e.message}");
      rethrow;
    } catch (e) {
      print("ApiService.getParticipaciones Error: $e");
      rethrow;
    }
  }

  Future<String?> actualizarParticipacion(
    int viajeId,
    int id,
    String token, {
    double? costo,
    bool? pagado,
  }) async {
    if (token.isEmpty) return 'Error: No hay sesión activa';
    try {
      final data = <String, dynamic>{};
      if (costo != null) data['costo_individual'] = costo;
      if (pagado != null) data['pagado'] = pagado;

      final res = await _dio.put(
        "/viajes/$viajeId/participaciones/$id",
        data: data,
        options: Options(
          headers: {'Authorization': "Bearer $token"},
          validateStatus: (status) => status! < 500,
        ),
      );
      if (res.statusCode == 200) return null;
      final dynamic resp = res.data;
      final detail = (resp is Map) ? resp['detail'] : resp;
      return 'Servidor dice: ${detail ?? "Error desconocido (${res.statusCode})"}';
    } on DioException catch (e) {
      return _handleDioError(e);
    } catch (e) {
      return 'Error inesperado: $e';
    }
  }

  Future<String?> eliminarPersona(int viajeId, int id, String token) async {
    try {
      await _dio.delete(
        "/viajes/$viajeId/personas/$id",
        options: _authOptions(token),
      );
      return null;
    } on DioException catch (e) {
      return _handleDioError(e);
    } catch (e) {
      return e.toString();
    }
  }

  Future<String?> eliminarActividad(int viajeId, int id, String token) async {
    try {
      await _dio.delete(
        "/viajes/$viajeId/actividades/$id",
        options: _authOptions(token),
      );
      return null;
    } on DioException catch (e) {
      return _handleDioError(e);
    } catch (e) {
      return e.toString();
    }
  }

  String _handleDioError(DioException e) {
    if (e.response?.statusCode == 401) {
      return 'Error 401: Sesión expirada o no autorizada.';
    }
    if (e.response?.statusCode == 403) {
      return 'Error 403: No tienes permiso para esta acción.';
    }
    if (e.response?.statusCode == 400) {
      return 'Error 400: Datos incorrectos.';
    }

    final data = e.response?.data;
    if (data is Map) {
      final detail = data['detail'];
      return detail?.toString() ?? e.message ?? 'Error de red';
    }
    return data?.toString() ?? e.message ?? 'Error de red';
  }

  String _connectionHint(DioException e) {
    if (e.type == DioExceptionType.connectionError ||
        e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout) {
      return 'No se puede conectar a ${ApiConfig.baseUrl}. '
          'Verifica que el backend esté activo con '
          'uvicorn main:app --host 0.0.0.0 --port 8000';
    }
    return _handleDioError(e);
  }
}