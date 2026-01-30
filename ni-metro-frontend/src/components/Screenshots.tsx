import { Container, Title, Text, Stack, SimpleGrid, Paper } from '@mantine/core';

export function Screenshots() {
  return (
    <section style={{ padding: '80px 0', backgroundColor: '#f8f9fa' }}>
      <Container size="lg">
        <Stack align="center" gap="xl" mb="xl">
          <Title order={2}>Скриншоты</Title>
          <Text size="lg" c="dimmed" style={{ textAlign: 'center', maxWidth: 600 }}>
            Посмотрите, как выглядит приложение
          </Text>
        </Stack>
        <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="xl">
          {[1, 2, 3].map((i) => (
            <Paper key={i} p="md" withBorder shadow="sm" style={{ aspectRatio: '9/16', background: '#e9ecef' }}>
              <Stack align="center" justify="center" style={{ height: '100%' }}>
                <Text c="dimmed">Скриншот {i}</Text>
              </Stack>
            </Paper>
          ))}
        </SimpleGrid>
      </Container>
    </section>
  );
}

