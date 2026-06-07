# Matriz de permisos — API v1 (implementada)

Provider de auth: Auth0 JWT (RS256 + JWKS). Los roles de viaje se consultan en `membresias_viaje`, no solo en claims.

## Roles por viaje (`RolViaje`)

- `ORGANIZADOR` — control total del viaje
- `JEFE_FAMILIA` — administra su familia dentro del viaje
- `MIEMBRO` — solo lectura de su familia

## Endpoints v1

| Endpoint | Método | Auth | Roles | Notas |
|---|---:|---|---|---|
| `/` | GET | No | — | Health check |
| `/status` | GET | No | — | Dashboard HTML |
| `/me` | GET | Sí | Cualquier miembro | Usuario + membresías |
| `/viajes` | GET | Sí | Miembro de cada viaje listado | Viajes del usuario |
| `/viajes` | POST | Sí | Cualquier autenticado | Crea viaje; usuario = ORGANIZADOR |
| `/viajes/{id}` | GET | Sí | Miembro del viaje | Detalle |
| `/viajes/{id}/resumen` | GET | Sí | Miembro | Global solo ORGANIZADOR |
| `/viajes/{id}/familias` | GET | Sí | Miembro | Filtrado por familia según rol |
| `/viajes/{id}/personas` | GET | Sí | Miembro | Solo familias visibles |
| `/viajes/{id}/personas` | POST | Sí | ORGANIZADOR, JEFE_FAMILIA | Jefe solo su familia |
| `/viajes/{id}/personas/admin` | POST | Sí | Admin global Auth0 | Sin membresía requerida |
| `/viajes/{id}/personas/{pid}` | DELETE | Sí | ORGANIZADOR, JEFE_FAMILIA | Soft-delete |
| `/viajes/{id}/actividades` | GET | Sí | Miembro | Filtrado por familia |
| `/viajes/{id}/actividades` | POST | Sí | ORGANIZADOR, JEFE_FAMILIA | Jefe solo su familia |
| `/viajes/{id}/actividades/{aid}` | DELETE | Sí | ORGANIZADOR, JEFE_FAMILIA | Jefe solo actividades de su familia |
| `/viajes/{id}/participaciones` | GET | Sí | Miembro | Filtrado por familia |
| `/viajes/{id}/participaciones/{pid}` | PUT | Sí | ORGANIZADOR, JEFE_FAMILIA | Editar monto/pagado |

## Legacy (compatibilidad temporal)

Requieren JWT. Usan `viaje_id=1` por defecto (query param opcional).

| Endpoint | Equivalente v1 |
|---|---|
| `GET /personas` | `GET /viajes/{id}/personas` |
| `POST /persona` | `POST /viajes/{id}/personas` |
| `POST /personas/admin` | `POST /viajes/{id}/personas/admin` |
| `GET /actividades` | `GET /viajes/{id}/actividades` |
| `POST /actividades` | `POST /viajes/{id}/actividades` |
| `GET /participaciones` | `GET /viajes/{id}/participaciones` |
| `PUT /participaciones/{id}` | `PUT /viajes/{id}/participaciones/{id}` |

## Admin global Auth0

Claim `admin` en: `role`, `roles`, o `https://appestable/role`.