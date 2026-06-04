# Ejemplo: crear persona como admin (frontend)

HTML mínimo con JavaScript `fetch` para enviar `POST /personas/admin`.

```html
<!doctype html>
<html>
  <body>
    <form id="persona-form">
      <input name="nombre" placeholder="Nombre" />
      <input name="email" placeholder="Email" />
      <input name="celular" placeholder="Celular" />
      <input name="familia_nombre" placeholder="Familia" />
      <label>Es jefe: <input type="checkbox" name="es_jefe" /></label>
      <button type="submit">Crear persona</button>
    </form>

    <script>
      const token = 'REEMPLAZA_CON_TOKEN_ADMIN'; // obtener del login

      document.getElementById('persona-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const form = e.target;
        const data = {
          nombre: form.nombre.value,
          email: form.email.value || null,
          celular: form.celular.value || null,
          familia_nombre: form.familia_nombre.value,
          es_jefe: form.es_jefe.checked,
          auth0_id: null
        };

        const res = await fetch('http://localhost:8000/personas/admin', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify(data)
        });

        if (res.ok) {
          const json = await res.json();
          alert('Persona creada: ' + json.id);
        } else {
          const err = await res.json();
          alert('Error: ' + (err.detail || JSON.stringify(err)));
        }
      });
    </script>
  </body>
</html>
```

Usa esta página localmente para probar: abrela en el navegador (necesitas un token de administrador válido). 
