import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'

type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'error'

type AuthState = {
  username: string | null
  status: AuthStatus
  error: string | null
}

type LoginRequest = {
  username: string
  password: string
}

const initialState: AuthState = {
  username: null,
  status: 'idle',
  error: null,
}

const toErrorMessage = async (response: Response): Promise<string> => {
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    const body = (await response.json()) as Record<string, unknown>
    const message = body.message
    if (typeof message === 'string' && message.trim().length > 0) {
      return message
    }
  }

  return response.statusText || 'Login failed'
}

export const login = createAsyncThunk<{ username: string }, LoginRequest, { rejectValue: string }>(
  'auth/login',
  async ({ username, password }, { rejectWithValue }) => {
    const response = await fetch('/api/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        username,
        password,
      }),
    })

    if (!response.ok) {
      return rejectWithValue(await toErrorMessage(response))
    }

    return { username }
  },
)

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.status = 'loading'
        state.error = null
      })
      .addCase(login.fulfilled, (state, action) => {
        state.status = 'authenticated'
        state.username = action.payload.username
        state.error = null
      })
      .addCase(login.rejected, (state, action) => {
        state.status = 'error'
        state.error = action.payload ?? 'Login failed. Please try again.'
      })
  },
})

export default authSlice.reducer
