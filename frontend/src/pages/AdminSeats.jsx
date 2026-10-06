import { useEffect, useState } from 'react';
import { AlertTriangle, Zap } from 'lucide-react';
import { seatApi } from '../api/seatApi';
import { capacityApi } from '../api/capacityApi';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';
import SeatGrid from '../components/SeatGrid';

const emptyForm = {
  seatNumber: '',
  floor: '',
  status: 'AVAILABLE',
};

const emptyCapacityForm = {
  floor: 'GROUND',
  maxSeats: '',
};

const supportedFloors = [
  { value: 'GROUND', label: 'Ground Floor' },
  { value: 'FIRST', label: 'First Floor' },
  { value: 'SECOND', label: 'Second Floor' },
  { value: 'THIRD', label: 'Third Floor' },
];

export default function AdminSeats() {
  const [seats, setSeats] = useState([]);
  const [capacities, setCapacities] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [capacityForm, setCapacityForm] = useState(emptyCapacityForm);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [settingCapacity, setSettingCapacity] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadData = async () => {
    try {
      const [seatsData, capacitiesData] = await Promise.all([
        seatApi.getAllSeats(),
        capacityApi.getFloorCapacities(),
      ]);
      setSeats(seatsData || []);
      setCapacities(capacitiesData || []);
    } catch (err) {
      setError(err.message || 'Unable to load seats and capacities.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleCapacityChange = (event) => {
    const { name, value } = event.target;
    setCapacityForm((current) => ({ ...current, [name]: value }));
  };

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');

    try {
      if (editingId) {
        await seatApi.updateSeat(editingId, form);
      } else {
        await seatApi.createSeat(form);
      }
      resetForm();
      await loadData();
      setSuccess('Seat saved successfully.');
    } catch (err) {
      setError(err.message || 'Unable to save seat.');
    }
  };

  const handleCapacitySubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');

    if (!capacityForm.maxSeats || parseInt(capacityForm.maxSeats) < 1 || parseInt(capacityForm.maxSeats) > 500) {
      setError('Maximum seats must be between 1 and 500.');
      return;
    }

    setSettingCapacity(true);
    try {
      const result = await capacityApi.setFloorCapacity(capacityForm.floor, parseInt(capacityForm.maxSeats));
      setSuccess(
        `Floor ${result.floor}: Set capacity to ${result.maxSeats} seats. Created ${result.createdSeats.length} new seats (total: ${result.totalSeats}).`
      );
      setCapacityForm(emptyCapacityForm);
      await loadData();
    } catch (err) {
      setError(err.message || 'Unable to set floor capacity.');
    } finally {
      setSettingCapacity(false);
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

    setError('');
    setSuccess('');

    try {
      await seatApi.deleteSeat(seat.id);
      await loadData();
      setSuccess(`Seat ${seat.seatNumber} deleted.`);
    } catch (err) {
      setError(err.message || 'Unable to delete seat.');
    }
  };

  if (loading) return <LoadingState message="Loading seats and capacities..." />;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Seat administration</p>
          <h1>Manage Seats & Floor Capacity</h1>
        </div>
      </header>

      {error && <ErrorMessage message={error} />}
      {success && (
        <div className="success-message">
          <div className="success-icon">✓</div>
          <div>{success}</div>
        </div>
      )}

      {/* Floor Capacity Management Section */}
      <section className="panel-card form-panel capacity-section">
        <div className="section-header">
          <Zap size={20} />
          <h2>Floor Capacity Management</h2>
        </div>
        <p className="section-description">
          Set maximum seating capacity per floor. Seats will be automatically created based on this capacity.
        </p>

        <form className="stack-form" onSubmit={handleCapacitySubmit}>
          <div className="two-column-form">
            <label>
              Floor
              <select name="floor" value={capacityForm.floor} onChange={handleCapacityChange} required>
                {supportedFloors.map((floor) => (
                  <option key={floor.value} value={floor.value}>
                    {floor.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Maximum Seats
              <input
                type="number"
                name="maxSeats"
                value={capacityForm.maxSeats}
                onChange={handleCapacityChange}
                min="1"
                max="500"
                placeholder="e.g., 20"
                required
              />
            </label>
          </div>

          <div className="card-actions">
            <button type="submit" disabled={settingCapacity} className="primary-button">
              {settingCapacity ? 'Setting Capacity...' : 'Set Floor Capacity'}
            </button>
          </div>
        </form>

        {capacities.length > 0 && (
          <div className="capacity-list">
            <h3>Current Floor Capacities</h3>
            <div className="capacity-grid">
              {capacities.map((capacity) => (
                <div key={capacity.floor} className="capacity-card">
                  <div className="capacity-floor">{capacity.floor}</div>
                  <div className="capacity-info">
                    <span className="capacity-max">{capacity.maxSeats} max seats</span>
                    <span className="capacity-total">{capacity.totalSeats} created</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </section>

      {/* Manual Seat Management Section */}
      <section className="panel-card form-panel">
        <div className="section-header">
          <AlertTriangle size={20} />
          <h2>{editingId ? 'Update seat' : 'Create seat'}</h2>
        </div>
        <p className="section-description">
          For advanced use cases. Typically, use floor capacity to auto-generate seats.
        </p>
        <form className="stack-form" onSubmit={handleSubmit}>
          <div className="two-column-form">
            <label>
              Seat number
              <input name="seatNumber" value={form.seatNumber} onChange={handleChange} required />
            </label>
            <label>
              Floor
              <select name="floor" value={form.floor} onChange={handleChange} required>
                <option value="">Select a floor</option>
                {supportedFloors.map((floor) => (
                  <option key={floor.value} value={floor.value}>
                    {floor.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Status
              <select name="status" value={form.status} onChange={handleChange}>
                <option value="AVAILABLE">AVAILABLE</option>
                <option value="BOOKED">BOOKED</option>
                <option value="MAINTENANCE">MAINTENANCE</option>
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

      {/* Seats Display Section */}
      <section className="panel-card">
        <h2>All Seats ({seats.length})</h2>
        <SeatGrid seats={seats} isAdmin onEdit={handleEdit} onDelete={handleDelete} />
      </section>
    </div>
  );
}
