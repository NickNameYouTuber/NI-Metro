import { Container, Group, Text, Stack } from '@mantine/core';

export function Footer() {
  return (
    <footer style={{ borderTop: '1px solid #e9ecef', padding: '40px 0', marginTop: '80px' }}>
      <Container size="lg">
        <Group justify="space-between" align="flex-start">
          <Stack gap="xs">
            <Text fw={600}>NI-Metro</Text>
            <Text size="sm" c="dimmed">
              Интерактивное приложение для метро
            </Text>
          </Stack>
          <Stack gap="xs">
            <Text fw={600}>Ссылки</Text>
            <Text size="sm" component="a" href="/docs" style={{ color: 'inherit', textDecoration: 'none' }}>
              Документация для разработчиков
            </Text>
            <Text size="sm" component="a" href="https://github.com" target="_blank" style={{ color: 'inherit', textDecoration: 'none' }}>
              GitHub
            </Text>
          </Stack>
          <Stack gap="xs">
            <Text fw={600}>Контакты</Text>
            <Text size="sm" c="dimmed">
              support@nicorp.com
            </Text>
          </Stack>
        </Group>
        <Text size="sm" c="dimmed" mt="xl" style={{ textAlign: 'center' }}>
          © 2024 NI-Metro. Все права защищены.
        </Text>
      </Container>
    </footer>
  );
}

