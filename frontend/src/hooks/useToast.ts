import { useState, useCallback } from 'react';

export interface ToastOptions {
  duration?: number; // in ms
}

export function useToast(defaultDuration: number = 2000) {
  const [show, setShow] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const triggerToast = useCallback((msg: string, options?: ToastOptions) => {
    setMessage(msg);
    setShow(true);
    setTimeout(() => {
      setShow(false);
      setMessage(null);
    }, options?.duration ?? defaultDuration);
  }, [defaultDuration]);

  return { show, message, triggerToast };
}
