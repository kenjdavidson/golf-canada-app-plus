import { Avatar, Box, Container, Link, Paper, Stack, Typography } from '@mui/material'
import LoginForm from './features/auth/LoginForm.tsx'
import { useAppSelector } from './app/hooks.ts'
import { selectIsAuthenticated, selectUsername } from './features/auth/selectors.ts'

function App() {
  const isAuthenticated = useAppSelector(selectIsAuthenticated)
  const username = useAppSelector(selectUsername)

  return (
    <Container maxWidth="sm">
      <Stack spacing={3} sx={{ py: 4 }}>
        <Paper elevation={2} sx={{ p: 3 }}>
          <Stack spacing={2} sx={{ alignItems: 'center' }}>
            <Avatar sx={{ width: 72, height: 72, bgcolor: 'primary.main', fontSize: '1.25rem' }}>
              GC+
            </Avatar>
            <Typography variant="h4" component="h1" align="center">
              Golf Canada App+
            </Typography>
            <Box sx={{ width: '100%' }}>
              <LoginForm />
            </Box>
            {isAuthenticated ? (
              <Typography variant="body2" color="success.main">
                Logged in as {username}
              </Typography>
            ) : null}
            <Typography variant="body2" color="text.secondary" align="center">
              <Link
                href="https://www.golfcanada.ca"
                target="_blank"
                rel="noreferrer"
                underline="hover"
              >
                Visit Golf Canada
              </Link>{' '}
              (no affiliation)
            </Typography>
            <Typography variant="body2" align="center">
              <Link href="/docs/PRIVACY_POLICY.md" underline="hover">
                Privacy Policy
              </Link>
            </Typography>
          </Stack>
        </Paper>
      </Stack>
    </Container>
  )
}

export default App
