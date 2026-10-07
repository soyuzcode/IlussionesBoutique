import React, { useState } from 'react';

interface Cita {
  id: number;
  nombre: string;
  fecha: string;
  hora: string;
  servicio: string;
}

const styles = {
  bodyWrapper: {
    backgroundColor: '#f4f5f7',
    minHeight: '100vh',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    padding: '20px',
    fontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
    color: '#333',
  },
  card: {
    backgroundColor: '#ffffff',
    width: '100%',
    maxWidth: '480px',
    borderRadius: '20px',
    boxShadow: '0 10px 25px rgba(0,0,0,0.08)',
    padding: '30px',
    boxSizing: 'border-box' as 'border-box',
  },
  header: {
    textAlign: 'center' as 'center',
    marginBottom: '20px',
  },
  logoBadge: {
    backgroundColor: '#d81b60',
    color: '#fff',
    fontSize: '12px',
    fontWeight: 'bold' as 'bold',
    padding: '4px 12px',
    borderRadius: '12px',
    display: 'inline-block',
    marginBottom: '8px',
    textTransform: 'uppercase' as 'uppercase',
  },
  title: {
    margin: '0',
    color: '#880e4f',
    fontSize: '24px',
  },
  subTitle: {
    margin: '5px 0 0 0',
    color: '#666',
    fontSize: '13px',
  },
  formContainer: {
    backgroundColor: '#fafafa',
    border: '1px solid #f0f0f0',
    borderRadius: '12px',
    padding: '18px',
    marginBottom: '25px',
  },
  sectionTitle: {
    margin: '0 0 15px 0',
    color: '#ad1457',
    fontSize: '16px',
    fontWeight: 'bold' as 'bold',
  },
  errorAlert: {
    backgroundColor: '#ffebee',
    color: '#c62828',
    padding: '10px 12px',
    borderRadius: '8px',
    fontSize: '13px',
    marginBottom: '15px',
    borderLeft: '4px solid #ef5350',
  },
  inputGroup: {
    marginBottom: '12px',
  },
  label: {
    display: 'block',
    fontSize: '12px',
    fontWeight: 'bold' as 'bold',
    color: '#444',
    marginBottom: '4px',
  },
  input: {
    width: '100%',
    padding: '10px 12px',
    borderRadius: '8px',
    border: '1px solid #ccc',
    fontSize: '14px',
    boxSizing: 'border-box' as 'border-box',
    outline: 'none',
    backgroundColor: '#fff',
    color: '#333',
  },
  row: {
    display: 'flex',
    gap: '10px',
  },
  btnPrimary: {
    width: '100%',
    backgroundColor: '#d81b60',
    color: '#fff',
    border: 'none',
    padding: '12px',
    borderRadius: '25px',
    fontSize: '14px',
    fontWeight: 'bold' as 'bold',
    cursor: 'pointer',
    marginTop: '10px',
  },
  btnSecondary: {
    backgroundColor: '#e0e0e0',
    color: '#333',
    border: 'none',
    padding: '12px',
    borderRadius: '25px',
    fontSize: '14px',
    cursor: 'pointer',
    marginTop: '10px',
  },
  citaItem: {
    border: '1px solid #f0f0f0',
    borderRadius: '10px',
    padding: '12px 15px',
    marginBottom: '10px',
    backgroundColor: '#fff',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    boxShadow: '0 2px 4px rgba(0,0,0,0.02)',
  },
  citaInfo: {
    fontSize: '13px',
  },
  btnEdit: {
    backgroundColor: '#1e88e5',
    color: '#fff',
    border: 'none',
    padding: '6px 14px',
    borderRadius: '6px',
    fontSize: '12px',
    cursor: 'pointer',
    fontWeight: 'bold' as 'bold',
  }
};

export function App() {
  const fechaHoyTexto = new Date().toLocaleDateString('es-ES', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  const [citas, setCitas] = useState<Cita[]>([
    { id: 1, nombre: 'Sofía Alvarado', fecha: '2026-10-06', hora: '14:00', servicio: 'Primera toma de medidas' },
    { id: 2, nombre: 'Valeria Gómez', fecha: '2026-10-07', hora: '16:30', servicio: 'Prueba de vestido' }
  ]);

  const [nombre, setNombre] = useState('');
  const [fecha, setFecha] = useState('');
  const [hora, setHora] = useState('');
  const [servicio, setServicio] = useState('');
  
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [mensajeError, setMensajeError] = useState('');

  const formatearFecha = (fechaStr: string) => {
    if (!fechaStr) return '';
    const partes = fechaStr.split('-');
    if (partes.length !== 3) return fechaStr;
    return `${partes[2]}/${partes[1]}/${partes[0]}`;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!nombre.trim() || !fecha.trim() || !hora.trim() || !servicio.trim()) {
      setMensajeError('Faltan datos por completar o el campo es inválido.');
      return;
    }

    setMensajeError('');

    if (editandoId !== null) {
      setCitas(citas.map(c => c.id === editandoId ? { id: editandoId, nombre, fecha, hora, servicio } : c));
      setEditandoId(null);
    } else {
      const nuevaCita: Cita = {
        id: Date.now(),
        nombre,
        fecha,
        hora,
        servicio,
      };
      setCitas([...citas, nuevaCita]);
    }

    setNombre('');
    setFecha('');
    setHora('');
    setServicio('');
  };

  const handleEditar = (cita: Cita) => {
    setEditandoId(cita.id);
    setNombre(cita.nombre);
    setFecha(cita.fecha);
    setHora(cita.hora);
    setServicio(cita.servicio);
    setMensajeError('');
  };

  const cancelarEdicion = () => {
    setEditandoId(null);
    setNombre('');
    setFecha('');
    setHora('');
    setServicio('');
    setMensajeError('');
  };

  return (
    <div style={styles.bodyWrapper}>
      <div style={styles.card}>
        
        {/* Encabezado */}
        <header style={styles.header}>
          <span style={styles.logoBadge}>Ilussiones</span>
          <h1 style={styles.title}>Boutique</h1>
          <p style={styles.subTitle}>📅 Hoy es: <strong>{fechaHoyTexto}</strong></p>
        </header>

        {/* Formulario */}
        <section style={styles.formContainer}>
          <h3 style={styles.sectionTitle}>{editandoId ? '✏️ Editar Cita' : '📌 Agendar Nueva Cita'}</h3>

          {mensajeError && (
            <div style={styles.errorAlert}>
              ⚠️ {mensajeError}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div style={styles.inputGroup}>
              <label style={styles.label}>Nombre Cliente:</label>
              <input
                type="text"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                placeholder="Ej. María López"
                style={styles.input}
              />
            </div>

            <div style={styles.row}>
              <div style={{ ...styles.inputGroup, flex: 1 }}>
                <label style={styles.label}>Fecha:</label>
                <input
                  type="date"
                  value={fecha}
                  onChange={(e) => setFecha(e.target.value)}
                  style={styles.input}
                />
              </div>

              <div style={{ ...styles.inputGroup, flex: 1 }}>
                <label style={styles.label}>Hora:</label>
                <input
                  type="time"
                  value={hora}
                  onChange={(e) => setHora(e.target.value)}
                  style={styles.input}
                />
              </div>
            </div>

            <div style={styles.inputGroup}>
              <label style={styles.label}>Servicio / Detalle:</label>
              <input
                type="text"
                value={servicio}
                onChange={(e) => setServicio(e.target.value)}
                placeholder="Ej. Prueba de vestido"
                style={styles.input}
              />
            </div>

            <div style={{ display: 'flex', gap: '10px' }}>
              <button type="submit" style={styles.btnPrimary}>
                {editandoId ? 'Guardar Cambios' : 'Agendar Cita'}
              </button>
              {editandoId && (
                <button type="button" onClick={cancelarEdicion} style={styles.btnSecondary}>
                  Cancelar
                </button>
              )}
            </div>
          </form>
        </section>

        {/* Lista de Citas */}
        <section>
          <h3 style={styles.sectionTitle}>📋 Citas Agendadas</h3>
          {citas.map((cita) => (
            <div key={cita.id} style={styles.citacitaItem as any}>
              <div style={styles.citaInfo}>
                <strong style={{ color: '#880e4f', fontSize: '14px' }}>{cita.nombre}</strong>
                <div style={{ color: '#555', marginTop: '2px' }}>
                  📅 {formatearFecha(cita.fecha)} | ⏰ {cita.hora}
                </div>
                <div style={{ color: '#888', fontSize: '12px' }}>{cita.servicio}</div>
              </div>

              <button onClick={() => handleEditar(cita)} style={styles.btnEdit}>
                Editar
              </button>
            </div>
          ))}
        </section>

      </div>
    </div>
  );
}

export default App;