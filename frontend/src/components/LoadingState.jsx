export default function LoadingState({ message = 'Loading...' }) {
  return (
    <div className="loading-card">
      <div className="spinner" aria-hidden="true" />
      <span>{message}</span>
    </div>
  );
}
