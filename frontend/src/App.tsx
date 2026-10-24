import { Button, Caption, Surface, Text, Title } from 'react-native-paper'
import { SafeAreaView, ScrollView, StyleSheet, View } from 'react-native'
import LoginForm from './components/auth/LoginForm.tsx'
import { useAppSelector } from './app/hooks.ts'
import { selectIsAuthenticated, selectUsername } from './store/auth/selectors.ts'
import { useAppTheme } from './context/AppThemeContext.tsx'

function App() {
  const isAuthenticated = useAppSelector(selectIsAuthenticated)
  const username = useAppSelector(selectUsername)
  const { isDarkTheme, toggleTheme, theme } = useAppTheme()

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: theme.colors.background }]}>
      <ScrollView contentContainerStyle={styles.container}>
        <Surface style={[styles.card, { backgroundColor: theme.colors.surface }]} elevation={2}>
          <View style={styles.header}>
            <Button
              mode="outlined"
              onPress={toggleTheme}
              style={styles.themeToggle}
              accessibilityLabel={isDarkTheme ? 'Switch to light mode' : 'Switch to dark mode'}
            >
              {isDarkTheme ? 'Light mode' : 'Dark mode'}
            </Button>
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>GC+</Text>
            </View>
            <Title style={styles.title}>Golf Canada App+</Title>
            <View style={styles.formContainer}>
              <LoginForm />
            </View>
            {isAuthenticated ? <Caption>Logged in as {username}</Caption> : null}
            <Text style={styles.linkText}>
              Visit Golf Canada{' '}
              <Text
                style={{ color: theme.colors.primary }}
                onPress={() => window.open('https://www.golfcanada.ca', '_blank', 'noreferrer')}
              >
                (no affiliation)
              </Text>
            </Text>
            <Text
              style={[styles.linkText, { color: theme.colors.primary }]}
              onPress={() =>
                window.open(
                  'https://kenjdavidson.com/golf-canada-app-plus/docs/PRIVACY_POLICY.md',
                  '_blank',
                  'noreferrer',
                )
              }
            >
              Privacy Policy
            </Text>
          </View>
        </Surface>
      </ScrollView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  container: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: 24,
  },
  card: {
    borderRadius: 20,
    padding: 20,
  },
  header: {
    alignItems: 'center',
    gap: 12,
  },
  themeToggle: {
    alignSelf: 'flex-end',
  },
  avatar: {
    width: 72,
    height: 72,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 36,
    backgroundColor: '#1a6b3c',
  },
  avatarText: {
    color: '#fff',
    fontSize: 20,
    fontWeight: '700',
  },
  title: {
    textAlign: 'center',
  },
  formContainer: {
    width: '100%',
  },
  linkText: {
    textAlign: 'center',
  },
})

export default App
