// Utility do obsługi API z globalną obsługą błędów

// Automatyczne wykrywanie adresu API na podstawie hosta frontendu
// Jeśli frontend działa na http://192.168.1.100:5173, to API będzie na http://192.168.1.100:8080
// Jeśli frontend działa na localhost, to API będzie na localhost
export const getApiBaseUrl = () => {
  const hostname = window.location.hostname;
  const protocol = window.location.protocol;
  // Użyj tego samego hosta co frontend, ale port 8080 dla API
  return `${protocol}//${hostname}:8080/api`;
};

const API_BASE_URL = getApiBaseUrl();

export class ApiError extends Error {
  constructor(message, status, response) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.response = response;
  }
}

export async function apiRequest(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  
  const defaultOptions = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const config = {
    ...defaultOptions,
    ...options,
    headers: {
      ...defaultOptions.headers,
      ...options.headers,
    },
  };

  try {
    const response = await fetch(url, config);
    
    // Sprawdź czy odpowiedź jest OK
    if (!response.ok) {
      let errorMessage = `Błąd ${response.status}`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.error || errorMessage;
      } catch (e) {
        // Jeśli nie można sparsować JSON, użyj domyślnego komunikatu
      }
      throw new ApiError(errorMessage, response.status, response);
    }

    // Spróbuj sparsować JSON
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return await response.json();
    }
    
    return await response.text();
  } catch (error) {
    // Jeśli to błąd sieci (np. backend nie działa)
    if (error instanceof TypeError && error.message === 'Failed to fetch') {
      throw new ApiError(
        'Brak połączenia z serwerem. Upewnij się, że backend jest uruchomiony.',
        0,
        null
      );
    }
    
    // Jeśli to już ApiError, przekaż dalej
    if (error instanceof ApiError) {
      throw error;
    }
    
    // Inny błąd
    throw new ApiError(error.message || 'Nieznany błąd', 0, null);
  }
}

// Helper do wyświetlania błędów użytkownikowi
export function handleApiError(error, showToast = null) {
  if (error instanceof ApiError) {
    const message = error.status === 0 
      ? 'Brak połączenia z serwerem. Upewnij się, że backend jest uruchomiony.'
      : error.message;
    
    if (showToast) {
      showToast(message, 'error', 5000);
    } else {
      alert(message);
    }
  } else {
    const message = 'Wystąpił nieoczekiwany błąd.';
    if (showToast) {
      showToast(message, 'error', 5000);
    } else {
      alert(message);
    }
  }
}


