// Lightweight singleton that breaks the client ↔ authStore circular dependency.
// authStore writes here on every token change; client reads here per request.
let _accessToken: string | null = null;

export const tokenRegistry = {
  get: () => _accessToken,
  set: (token: string | null) => {
    _accessToken = token;
  },
};
