import { useState, useEffect } from 'react';
import Toast from './Toast';
import './ToastContainer.css';

let toastIdCounter = 0;
let toastListeners = [];

export const showToast = (message, type = 'info', duration = 3000) => {
  const id = toastIdCounter++;
  toastListeners.forEach(listener => listener({ id, message, type, duration }));
  return id;
};

export default function ToastContainer() {
  const [toasts, setToasts] = useState([]);

  useEffect(() => {
    const listener = (toast) => {
      setToasts(prev => [...prev, toast]);
    };
    toastListeners.push(listener);

    return () => {
      toastListeners = toastListeners.filter(l => l !== listener);
    };
  }, []);

  const removeToast = (id) => {
    setToasts(prev => prev.filter(toast => toast.id !== id));
  };

  return (
    <div className="toast-container">
      {toasts.map((toast, index) => (
        <div 
          key={toast.id} 
          style={{ 
            position: 'absolute', 
            top: `${20 + index * 80}px`,
            right: '20px'
          }}
        >
          <Toast
            message={toast.message}
            type={toast.type}
            duration={toast.duration}
            onClose={() => removeToast(toast.id)}
          />
        </div>
      ))}
    </div>
  );
}


