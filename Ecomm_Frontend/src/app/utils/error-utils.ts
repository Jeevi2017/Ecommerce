export function getFriendlyError(error: any, fallback = 'An error occurred'): string {
  if (!error) return fallback;
  if (typeof error === 'string') return error;
  // If service returned normalized error with 'message'
  if (error.message) return error.message;
  // If backend returned structured error
  if (error.error && typeof error.error === 'string') return error.error;
  if (error.error && error.error.message) return error.error.message;
  // Network or unknown
  return fallback;
}
