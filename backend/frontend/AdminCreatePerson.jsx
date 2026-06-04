import React, { useState } from 'react';

/**
 * AdminCreatePerson
 * Props:
 * - token (optional): admin JWT token. If not provided, component reads `access_token` from localStorage.
 *
 * Usage:
 * <AdminCreatePerson token={adminToken} />
 */
export default function AdminCreatePerson({ token, onCreated }) {
  const [form, setForm] = useState({ nombre: '', email: '', celular: '', familia_nombre: '', es_jefe: false });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(null);

  const authToken = token || localStorage.getItem('access_token');

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((s) => ({ ...s, [name]: type === 'checkbox' ? checked : value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage(null);

    if (!authToken) {
      setMessage({ type: 'error', text: 'Token admin no disponible. Coloca el token como prop o en localStorage.access_token' });
      setLoading(false);
      return;
    }

    const payload = { ...form, auth0_id: null };

    try {
      const res = await fetch('http://localhost:8000/personas/admin', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`,
        },
        body: JSON.stringify(payload),
      });

      if (res.ok) {
        const data = await res.json();
        setMessage({ type: 'success', text: `Persona creada (id: ${data.id})` });
        setForm({ nombre: '', email: '', celular: '', familia_nombre: '', es_jefe: false });
        // Llamar callback si se proporcionó
        if (typeof onCreated === 'function') onCreated(data);
        // Disparar evento global para que la app pueda refrescar la lista
        try { window.dispatchEvent(new CustomEvent('personas:updated', { detail: data })); } catch(e){}
      } else {
        const err = await res.json().catch(() => ({}));
        setMessage({ type: 'error', text: err.detail || JSON.stringify(err) });
      }
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 520 }}>
      <h3>Crear Persona (Admin)</h3>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Nombre</label>
          <input name="nombre" value={form.nombre} onChange={handleChange} required />
        </div>
        <div>
          <label>Email</label>
          <input name="email" value={form.email} onChange={handleChange} type="email" />
        </div>
        <div>
          <label>Celular</label>
          <input name="celular" value={form.celular} onChange={handleChange} />
        </div>
        <div>
          <label>Familia</label>
          <input name="familia_nombre" value={form.familia_nombre} onChange={handleChange} required />
        </div>
        <div>
          <label>
            <input name="es_jefe" checked={form.es_jefe} onChange={handleChange} type="checkbox" /> Es jefe
          </label>
        </div>
        <div style={{ marginTop: 8 }}>
          <button type="submit" disabled={loading}>{loading ? 'Creando...' : 'Crear persona'}</button>
        </div>
      </form>
      {message && (
        <div style={{ marginTop: 12, color: message.type === 'error' ? 'crimson' : 'green' }}>
          {message.text}
        </div>
      )}
    </div>
  );
}
