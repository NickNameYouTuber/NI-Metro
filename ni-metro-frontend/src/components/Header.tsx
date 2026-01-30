import { Container, Group, Button, Title } from '@mantine/core';

export function Header() {
  return (
    <header style={{ borderBottom: '1px solid #e9ecef', padding: '16px 0' }}>
      <Container size="lg">
        <Group justify="space-between">
          <Title order={3}>NI-Metro</Title>
          <Group>
            <Button variant="subtle" component="a" href="https://github.com" target="_blank">
              GitHub
            </Button>
            <Button variant="subtle" component="a" href="/docs">
              Документация
            </Button>
          </Group>
        </Group>
      </Container>
    </header>
  );
}

