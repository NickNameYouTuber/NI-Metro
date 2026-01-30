import { Container, Title, Text, SimpleGrid, Paper, Stack } from '@mantine/core';

const features = [
  {
    title: 'Интерактивные карты',
    description: 'Детальные карты метро с возможностью поиска станций и построения маршрутов',
  },
  {
    title: 'Актуальные оповещения',
    description: 'Уведомления о работах на станциях, закрытых выходах и изменениях в расписании',
  },
  {
    title: 'Информация о станциях',
    description: 'Подробная информация о каждой станции: расписание работы, количество выходов и лифтов',
  },
  {
    title: 'Офлайн режим',
    description: 'Работает без интернета - все карты загружаются локально на устройство',
  },
];

export function Features() {
  return (
    <section style={{ padding: '80px 0' }}>
      <Container size="lg">
        <Stack align="center" gap="xl" mb="xl">
          <Title order={2}>Преимущества</Title>
          <Text size="lg" c="dimmed" style={{ textAlign: 'center', maxWidth: 600 }}>
            Все необходимое для комфортного путешествия по метро
          </Text>
        </Stack>
        <SimpleGrid cols={{ base: 1, sm: 2, md: 4 }} spacing="xl">
          {features.map((feature, index) => (
            <Paper key={index} p="xl" withBorder shadow="sm">
              <Stack gap="md">
                <Title order={4}>{feature.title}</Title>
                <Text c="dimmed">{feature.description}</Text>
              </Stack>
            </Paper>
          ))}
        </SimpleGrid>
      </Container>
    </section>
  );
}

