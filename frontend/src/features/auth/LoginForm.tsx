import { FormEvent, useState } from 'react'
import {
  Alert,
  Button,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
} from '@mui/material'
import { Visibility, VisibilityOff } from '@mui/icons-material'
import { useAppDispatch, useAppSelector } from '../../app/hooks.ts'
import { login } from './authSlice.ts'
import { selectAuthError, selectAuthStatus } from './selectors.ts'

const LoginForm = () => {
  const dispatch = useAppDispatch()
  const authStatus = useAppSelector(selectAuthStatus)
  const authError = useAppSelector(selectAuthError)

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void dispatch(login({ username, password }))
  }

  const isLoading = authStatus === 'loading'

  return (
    <Stack component="form" spacing={2} onSubmit={handleSubmit}>
      <TextField
        label="Username"
        type="text"
        value={username}
        onChange={(event) => setUsername(event.target.value)}
        required
        fullWidth
        autoComplete="username"
      />
      <TextField
        label="Password"
        type={showPassword ? 'text' : 'password'}
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        required
        fullWidth
        autoComplete="current-password"
        slotProps={{
          input: {
            endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  onClick={() => setShowPassword((current) => !current)}
                  edge="end"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <VisibilityOff /> : <Visibility />}
                </IconButton>
              </InputAdornment>
            ),
          },
        }}
      />
      {authError ? <Alert severity="error">{authError}</Alert> : null}
      <Button type="submit" variant="contained" size="large" disabled={isLoading}>
        {isLoading ? 'Logging in...' : 'Login'}
      </Button>
    </Stack>
  )
}

export default LoginForm
