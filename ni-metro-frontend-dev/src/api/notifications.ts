import { apiClient } from './client';

export interface NotificationData {
  id: string;
  type: 'normal' | 'important';
  triggerType: 'once' | 'date_range' | 'station' | 'line';
  triggerStationId?: string;
  triggerLineId?: string;
  triggerDateStart?: string;
  triggerDateEnd?: string;
  contentText?: string;
  contentImageUrl?: string;
  contentImageResource?: string;
  contentCaption?: string;
  createdAt?: string;
  updatedAt?: string;
  isActive?: boolean;
}

export const notificationsApi = {
  async getAll(options?: { stationId?: string; lineId?: string }): Promise<NotificationData[]> {
    const params = new URLSearchParams();
    if (options?.stationId) params.append('stationId', options.stationId);
    if (options?.lineId) params.append('lineId', options.lineId);

    const response = await apiClient.getClient().get<NotificationData[]>(
      `/notifications${params.toString() ? `?${params.toString()}` : ''}`
    );
    return response.data;
  },

  async getById(id: string): Promise<NotificationData> {
    const response = await apiClient.getClient().get<NotificationData>(`/notifications/${id}`);
    return response.data;
  },

  async create(notification: NotificationData): Promise<NotificationData> {
    const response = await apiClient.getClient().post<NotificationData>('/notifications', notification);
    return response.data;
  },

  async update(id: string, notification: NotificationData): Promise<NotificationData> {
    const response = await apiClient.getClient().put<NotificationData>(`/notifications/${id}`, notification);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await apiClient.getClient().delete(`/notifications/${id}`);
  },
};

