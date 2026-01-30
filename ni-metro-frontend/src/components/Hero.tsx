import { Container, Title, Text, Button, Stack, Group } from '@mantine/core';

export function Hero() {
  return (
    <section style={{ padding: '80px 0', background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', color: 'white' }}>
      <Container size="lg">
        <Stack align="center" gap="xl">
          <Title order={1} size="3.5rem" style={{ textAlign: 'center' }}>
            NI-Metro
          </Title>
          <Text size="xl" style={{ textAlign: 'center', maxWidth: 600 }}>
            Интерактивное приложение для навигации по метро с актуальными оповещениями о работах и изменениях
          </Text>
          <Group>
            <Button size="lg" variant="white" color="dark">
              Скачать для Android
            </Button>
            <Button size="lg" variant="outline" style={{ borderColor: 'white', color: 'white' }}>
              Узнать больше
            </Button>
          </Group>
        </Stack>
      </Container>
    </section>
  );
}

