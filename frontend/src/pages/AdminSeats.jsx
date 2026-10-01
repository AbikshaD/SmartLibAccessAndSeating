import { useEffect, useState } from 'react';
import { seatApi } from '../api/seatApi';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';
import SeatGrid from '../components/SeatGrid';

const emptyForm = {
  seatNumber: '',
  floor: '',
  status: 'AVAILABLE',
};

export default function AdminSeats() {
  const [seats, setSeats] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadSeats = async () => {
    try {
      const data = await seatApi.getAllSeats();
      setSeats(data || []);
    } catch (err) {
      setError(err.message || 'Unable to load seats.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSeats();
  }, []);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    try {
      if (editingId) {
        await seatApi.updateSeat(editingId, form);
      } else {
        await seatApi.createSeat(form);
      }
      resetForm();
      await loadSeats();
    } catch (err) {
      setError(err.message || 'Unable to save seat.');
    }
  };

  const handleEdit = (seat) => {
    setEditingId(seat.id);
    setForm({
      seatNumber: seat.seatNumber,
      floor: seat.floor,
      status: seat.status,
    });
  };

  const handleDelete = async (seat) => {
    const confirmed = window.confirm(`Delete seat ${seat.seatNumber}?`);
    if (!confirmed) return;

    try {
      await seatApi.deleteSeat(seat.id);
      await loadSeats();
    } catch (err) {
      setError(err.message || 'Unable to delete seat.');
    }
  };

  if (loading) return <LoadingState message="Loading seats..." />;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Seat administration</p>
          <h1>Manage Seats</h1>
        </div>
      </header>

      <ErrorMessage message={error} />

      <section className="panel-card form-panel">
        <h2>{editingId ? 'Update seat' : 'Create seat'}</h2>
        <form className="stack-form" onSubmit={handleSubmit}>
          <div className="two-column-form">
            <label>
              Seat number
              <input name="seatNumber" value={form.seatNumber} onChange={handleChange} required />
            </label>
            <label>
              Floor
              <input name="floor" value={form.floor} onChange={handleChange} required />
            </label>
            <label>
              Status
              <select name="status" value={form.status} onChange={handleChange}>
                <option value="AVAILABLE">AVAILABLE</option>
                <option value="OCCUPIED">OCCUPIED</option>
              </select>
            </label>
          </div>

          <div className="card-actions">
            <button type="submit" className="primary-button">
              {editingId ? 'Save changes' : 'Create seat'}
            </button>
            {editingId && (
              <button type="button" className="secondary" onClick={resetForm}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="panel-card">
        <SeatGrid seats={seats} isAdmin onEdit={handleEdit} onDelete={handleDelete} />
      </section>
    </div>
  );
}
