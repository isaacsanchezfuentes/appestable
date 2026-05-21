import 'package:dio/dio.dart';
import '../core/api_config.dart';
import '../models/persona.dart';
import '../models/actividad.dart';

class ApiService {
  final Dio _dio = Dio(BaseOptions(
    baseUrl: ApiConfig.baseUrl,
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 10),
  ));

  // --- PERSONAS ---
  Future<List<Persona>> getPersonas() async {
    try {
      final res = await _dio.get('/personas');
      return (res.data as List).map((p) => Persona.fromJson(p)).toList();
    } catch (e) {
      print("GET PERSONAS ERROR: $e");
      return [];
    }
  }

  Future<String?> registrarPersona(Persona p, String token, {bool isAdmin = false}) async {
    try {
      final path = isAdmin ? '/personas/admin' : '/persona';
      final res = await _dio.post(
        path,
        data: p.toJson(),
        options: Options(headers: {'Authorization': 'Bearer $token'}),
      );
      if (res.statusCode == 200 || res.statusCode == 201) return null;
      return "Error: ${res.statusCode}";
    } on DioException catch (e) {
      return e.response?.data?['detail']?.toString() ?? e.message;
    } catch (e) { return e.toString(); }
  }

  // --- ACTIVIDADES ---
  Future<List<Actividad>> getActividades() async {
    try {
      final res = await _dio.get('/actividades');
      return (res.data as List).map((a) => Actividad.fromJson(a)).toList();
    } catch (e) { return []; }
  }

  Future<String?> registrarActividad(String nombre, double costo, String fecha, List<int> pIds, String token) async {
    try {
      final res = await _dio.post(
        '/actividades',
        data: {
          'nombre': nombre,
          'costo_total': costo,
          'fecha': fecha,
          'participantes_ids': pIds,
        },
        options: Options(headers: {'Authorization': 'Bearer $token'}),
      );
      if (res.statusCode == 201) return null;
      return "Error al guardar actividad";
    } on DioException catch (e) {
      return e.response?.data?['detail']?.toString() ?? e.message;
    } catch (e) { return e.toString(); }
  }
}
