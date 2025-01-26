
import '@mantine/core/styles.css';
import { MantineProvider } from '@mantine/core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { HomePage } from './pages/Home';
import { UsersPage } from './pages/admin/users';
import { AssignmentsPage } from './pages/admin/assignments';
import Root from './templates/Root';

const queryClient = new QueryClient()

const router = createBrowserRouter([
  {
    element: <Root />,
    children: [
      {
        path: '/',
        element: <HomePage />,
      },
      {
        path: '/admin/assignments',
        element: <AssignmentsPage />,
      },
      {
        path: '/admin/users',
        element: <UsersPage />,
      },
    ]
  }
]);

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <MantineProvider>
        <RouterProvider router={router} />
      </MantineProvider>
    </QueryClientProvider>
  );
}
