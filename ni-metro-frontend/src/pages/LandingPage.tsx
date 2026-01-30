import { Container, Title, Text, Group, Button, Stack, SimpleGrid, Paper } from '@mantine/core';
import { Header } from '../components/Header';
import { Hero } from '../components/Hero';
import { Features } from '../components/Features';
import { Screenshots } from '../components/Screenshots';
import { Footer } from '../components/Footer';

export function LandingPage() {
  return (
    <div>
      <Header />
      <Hero />
      <Features />
      <Screenshots />
      <Footer />
    </div>
  );
}

