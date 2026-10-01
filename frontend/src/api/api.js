const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const normalizePath = (path) => (path.startsWith('/') ? path : `/${path}`);

const getErrorMessage = (status, payload) => {
  if (payload && typeof payload.error === 'string') {
    return payload.error;
  }

  switch (status) {
    case 400:
      return 'The submitted data is invalid.';
    case 401:
      return 'Your session has expired. Please log in again.';
    case 403:
      return 'You do not have permission to perform this action.';
    case 404:
      return 'Resource not found.';
    case 409:
      return 'An item with this information already exists.';
    default:
      return 'Something went wrong. Please try again.';
  }
};

export function getAuthToken() {
  return localStorage.getItem('smartlib_token');
}

export function setAuthToken(token) {
  localStorage.setItem('smartlib_token', token);
}

export function clearAuthToken() {
  localStorage.removeItem('smartlib_token');
}

export async function apiRequest(path, options = {}) {
  const {
    method = 'GET',
    body,
    headers = {},
    auth = true,
  } = options;

  const requestHeaders = { Accept: 'application/json', ...headers };

  if (!(body instanceof FormData)) {
    requestHeaders['Content-Type'] = 'application/json';
  }

  if (auth) {
    const token = getAuthToken();
    if (token) {
      requestHeaders.Authorization = `Bearer ${token}`;
    }
  }

  const response = await fetch(`${API_BASE_URL}${normalizePath(path)}`, {
    method,
    headers: requestHeaders,
    body: body === undefined || body === null ? undefined : body instanceof FormData ? body : JSON.stringify(body),
  });

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') || '';
  const payload = contentType.includes('application/json') ? await response.json().catch(() => null) : null;

  if (!response.ok) {
    throw new Error(getErrorMessage(response.status, payload));
  }

  return payload;
}
