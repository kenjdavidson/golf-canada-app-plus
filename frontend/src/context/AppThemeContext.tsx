import {
  createContext,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  MD3DarkTheme,
  MD3LightTheme,
  PaperProvider,
  type MD3Theme,
} from 'react-native-paper'

type AppThemeContextValue = {
  theme: MD3Theme
  isDarkTheme: boolean
  toggleTheme: () => void
}

const AppThemeContext = createContext<AppThemeContextValue | undefined>(undefined)

export function AppThemeProvider({ children }: { children: ReactNode }) {
  const [isDarkTheme, setIsDarkTheme] = useState(false)

  const theme = useMemo<MD3Theme>(() => {
    const baseTheme = isDarkTheme ? MD3DarkTheme : MD3LightTheme

    return {
      ...baseTheme,
      colors: {
        ...baseTheme.colors,
        primary: '#1a6b3c',
        secondary: '#2d7d46',
        surface: isDarkTheme ? '#1f1f1f' : '#ffffff',
        background: isDarkTheme ? '#121212' : '#f5f7f5',
      },
    }
  }, [isDarkTheme])

  const value = useMemo<AppThemeContextValue>(
    () => ({
      theme,
      isDarkTheme,
      toggleTheme: () => setIsDarkTheme((current) => !current),
    }),
    [isDarkTheme, theme],
  )

  return (
    <AppThemeContext.Provider value={value}>
      <PaperProvider theme={theme}>{children}</PaperProvider>
    </AppThemeContext.Provider>
  )
}

export function useAppTheme() {
  const context = useContext(AppThemeContext)

  if (!context) {
    throw new Error('useAppTheme must be used within an AppThemeProvider')
  }

  return context
}
