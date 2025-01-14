package com.nicorp.nimetro.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Transfer implements Parcelable {
    private List<Station> stations;
    private int time;
    private String type;
    private String transferMap;
    private Map<String, String> transitionTexts; // Тексты переходов между станциями
    private List<TransferRoute> transferRoutes; // Маршруты перехода

    public String getTransferMap() {
        return transferMap;
    }

    public void setTransferMap(String transferMap) {
        this.transferMap = transferMap;
    }

    public Transfer(List<Station> stations, int time, String type) {
        this.stations = stations;
        this.time = time;
        this.type = type;
        this.transitionTexts = generateTransitionTexts();
    }

    public Transfer(List<Station> stations, int time, String type, String transferMap, List<TransferRoute> transferRoutes) {
        this.stations = stations;
        this.time = time;
        this.type = type;
        this.transferMap = transferMap;
        this.transferRoutes = transferRoutes;
    }

    protected Transfer(Parcel in) {
        stations = in.createTypedArrayList(Station.CREATOR);
        time = in.readInt();
        type = in.readString();
        transferMap = in.readString();
        transitionTexts = new HashMap<>();
        in.readMap(transitionTexts, String.class.getClassLoader());
        transferRoutes = in.createTypedArrayList(TransferRoute.CREATOR);
    }

    public static final Creator<Transfer> CREATOR = new Creator<Transfer>() {
        @Override
        public Transfer createFromParcel(Parcel in) {
            return new Transfer(in);
        }

        @Override
        public Transfer[] newArray(int size) {
            return new Transfer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(stations);
        dest.writeInt(time);
        dest.writeString(type);
        dest.writeString(transferMap);
        dest.writeMap(transitionTexts);
        dest.writeTypedList(transferRoutes);
    }

    public List<Station> getStations() {
        return stations;
    }

    public int getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public List<TransferRoute> getTransferRoutes() {
        return transferRoutes;
    }

    /**
     * Генерирует тексты переходов между всеми станциями перехода.
     */
    private Map<String, String> generateTransitionTexts() {
        Map<String, String> texts = new HashMap<>();
        for (int i = 0; i < stations.size(); i++) {
            for (int j = 0; j < stations.size(); j++) {
                if (i != j) {
                    Station from = stations.get(i);
                    Station to = stations.get(j);
                    String key = from.getId() + "_to_" + to.getId();
                    String text = generateTransitionText(from, to);
                    texts.put(key, text);
                }
            }
        }
        return texts;
    }

    /**
     * Генерирует текст перехода между двумя станциями.
     */
    private String generateTransitionText(Station from, Station to) {
        if ("crossplatform".equals(type)) {
            return "Для этого перейдите на противоположную сторону платформы.";
        } else if ("escalator".equals(type)) {
            return "Для этого Поднимитесь/спуститесь по эскалатору.";
        } else if ("walking".equals(type)) {
            return "Пройдите по переходу от станции " + from.getName() + " до станции " + to.getName() + ".";
        } else {
            return "Перейдите от станции " + from.getName() + " до станции " + to.getName() + ".";
        }
    }

    /**
     * Возвращает текст перехода между двумя станциями.
     */
    public String getTransitionText(Station from, Station to) {
        String key = from.getId() + "_to_" + to.getId();
        return transitionTexts.getOrDefault(key, "Переход недоступен.");
    }

    /**
     * Возвращает маршрут перехода между двумя станциями.
     */
    public TransferRoute getTransferRoute(Station prev, Station from, Station to) {
        for (TransferRoute route : transferRoutes) {
            Log.d("Transfer", "Route: " + route.getFrom() + " -> " + route.getTo() + " -> " + route.getPrev());
            Log.d("Transfer", "From: " + from.getName() + " To: " + to.getName() + " Prev: " + prev.getName());
            // Проверяем, подходит ли маршрут для текущего перехода
            if (route.getFrom().equals(from.getId()) && route.getTo().equals(to.getId())) {
                Log.d("Transfer", "Found route: " + route.getFrom());
                Log.d("Transfer", "Found route: " + route.getTo());
                Log.d("Transfer", "Found route: " + route.getPrev());
                // Если предыдущая станция указана, используем её для выбора варианта
                if (prev != null && route.getPrev() != null && route.getPrev().equals(prev.getId())) {
                    return route;
                }
                // Если предыдущая станция не указана, используем вариант по умолчанию
                else if (route.getPrev() == null) {
                    return route;
                }
            }
        }
        return null; // Если подходящий маршрут не найден
    }

    @Override
    public String toString() {
        return "Transfer{" +
                "stations=" + stations +
                ", time=" + time +
                ", type='" + type + '\'' +
                '}';
    }
}