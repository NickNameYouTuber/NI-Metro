import { apiClient } from './client';

export interface MapListItem {
  id: string;
  name: string;
  country?: string;
  version?: string;
  author?: string;
  iconUrl?: string;
  fileName: string;
  createdAt: string;
  updatedAt: string;
}

export interface MapData {
  id?: string;
  name: string;
  country?: string;
  version?: string;
  author?: string;
  iconUrl?: string;
  fileName: string;
  data: any; // JSON map data
  createdAt?: string;
  updatedAt?: string;
  isActive?: boolean;
}

export const mapsApi = {
  async getAll(): Promise<MapListItem[]> {
    const response = await apiClient.getClient().get<MapListItem[]>('/maps');
    return response.data;
  },

  async getById(id: string): Promise<MapData> {
    const response = await apiClient.getClient().get<MapData>(`/maps/${id}`);
    return response.data;
  },

  async getByFileName(fileName: string): Promise<MapData> {
    const response = await apiClient.getClient().get<MapData>(`/maps/by-name/${encodeURIComponent(fileName)}`);
    return response.data;
  },

  async create(map: MapData): Promise<MapData> {
    const response = await apiClient.getClient().post<MapData>('/maps', map);
    return response.data;
  },

  async update(id: string, map: MapData): Promise<MapData> {
    const response = await apiClient.getClient().put<MapData>(`/maps/${id}`, map);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await apiClient.getClient().delete(`/maps/${id}`);
  },
};

