# Integración Frontend: `AdminCreatePerson` component

Archivo: `frontend/AdminCreatePerson.jsx`

Resumen:

- Componente React funcional que envía `POST /personas/admin` con un JWT admin.
- Lee el token desde la prop `token` o desde `localStorage.access_token`.

Uso rápido:

1. Copiar `frontend/AdminCreatePerson.jsx` a vuestro proyecto React (p. ej. `src/components/AdminCreatePerson.jsx`).
2. Asegurarse de que el token de admin esté disponible. Opciones:
   - Pasarlo como prop: `<AdminCreatePerson token={adminToken} />`
   - Guardarlo en `localStorage.setItem('access_token', adminToken)` y usar `<AdminCreatePerson />`

3. Ejecutar y probar localmente; el backend por defecto usado es `http://localhost:8000`.

Notas:

- Si vuestro frontend corre en otro origen, aseguraos de que el backend permita CORS para esa URL.
- Si el token no tiene claim `admin`, el backend devolverá 403. El flujo de obtención del token queda a cargo del equipo de auth/frontend.
- El backend ahora soporta alias de debug para compatibilidad con frontends antiguos:
  - `GET /debug/personas` → `GET /personas`
  - `GET /debug/familias` → `GET /familias`
  - `GET /debug/actividades` → `GET /actividades`
- Sin embargo, lo ideal es usar las rutas estándar `/personas`, `/familias` y `/actividades`.
