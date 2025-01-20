
import '@mantine/core/styles.css';
import { MantineProvider } from '@mantine/core';
import { Router } from './Router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient()

export default function App() {
  return (
  <QueryClientProvider client={queryClient}>
    <MantineProvider>
      <Router />
    </MantineProvider>
  </QueryClientProvider>
  );
}
