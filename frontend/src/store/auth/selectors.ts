import type { RootState } from '../../app/store.ts'

export const selectAuthStatus = (state: RootState) => state.auth.status
export const selectAuthError = (state: RootState) => state.auth.error
export const selectIsAuthenticated = (state: RootState) => state.auth.status === 'authenticated'
export const selectUsername = (state: RootState) => state.auth.username
