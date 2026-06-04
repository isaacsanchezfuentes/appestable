# Matriz de permisos por endpoint

Este documento resume los permisos (Admin / Usuario autenticado / Público) para cada endpoint del backend.

- Provider de auth: Auth0 (ver `main.py`). Verificación de token por JWKS.
- Reglas admin: claim `role == "admin"` o `roles == "admin"` o `https://appestable/role == "admin"`, o tabla `users.role == 'admin'`.

| Endpoint | Método | Requiere token | Admin | Usuario autenticado | Público | Notas |
|---|---:|---:|---:|---:|---:|---|
| `/` | GET | No | No | No | Sí | Health/status |
| `/me` | GET | Sí | No | Sí | No | Devuelve claims del token |
| `/persona` | POST | Sí | No | Sí | No | Registro por usuario; usa `sub` como `auth0_id` |
| `/personas/admin` | POST | Sí | Sí | No | No | Crear persona para grupo (admin) |
| `/personas` | GET | No | Sí | Sí | Sí | Lista personas activas (`is_deleted == False`) |
| `/personas/{persona_id}` | GET | No | Sí | Sí | Sí | Obtener persona por id |
| `/personas/{persona_id}` | PUT | Sí | No | Sí | No | Actualizar persona (requiere token) |
| `/personas/{persona_id}` | DELETE | Sí | No | Sí | No | Soft-delete (`is_deleted = True`) |
| `/familias` | GET | No | Sí | Sí | Sí | Lista familias |
| `/familias/{familia_id}` | GET | No | Sí | Sí | Sí | Obtener familia por id |
| `/familias` | POST | Sí | Sí | No | No | Crear familia (admin o token válido) |
| `/familias/{familia_id}` | DELETE | Sí | Sí | No | No | Eliminar familia |
| `/actividades` | GET | No | Sí | Sí | Sí | Lista actividades |
| `/actividades/{actividad_id}` | GET | No | Sí | Sí | Sí | Obtener actividad por id |
| `/actividades` | POST | Sí | Sí | No | No | Crear actividad |
| `/actividades/{actividad_id}` | PUT | Sí | Sí | No | No | Actualizar actividad |
| `/actividades/{actividad_id}` | DELETE | Sí | Sí | No | No | Eliminar actividad |
| `/participaciones` | GET | No | Sí | Sí | Sí | Lista participaciones |
| `/actividades/{actividad_id}/participaciones` | GET | No | Sí | Sí | Sí | Participaciones por actividad |
| `/participaciones` | POST | Sí | No | Sí | No | Crear participación (requiere persona y actividad existentes) |
| `/participaciones/{participacion_id}` | PUT | Sí | No | Sí | No | Actualizar participación |
| `/participaciones/{participacion_id}` | DELETE | Sí | No | Sí | No | Eliminar participación |
| `/debug/personas` | GET | No | Sí | Sí | Sí | Alias para compatibilidad → `/personas` |
| `/debug/familias` | GET | No | Sí | Sí | Sí | Alias para compatibilidad → `/familias` |
| `/debug/actividades` | GET | No | Sí | Sí | Sí | Alias para compatibilidad → `/actividades` |

## Notas adicionales

- `auth0_id` en `personas` es nullable: se permite crear personas sin `auth0_id` (p. ej. creadas por admin).
- Se evita la creación de personas con emails duplicados (si `is_deleted == False`).
- DB URL: `postgresql://postgres:postgres123@localhost:5432/appestable` (ver `db/session.py`).
- Si necesitáis roles más granulares (p. ej. permiso por endpoint para `es_jefe`), lo podemos extender en esta matriz.

---
Generado automáticamente por el asistente (puede editarse para ampliar/ajustar permisos).
