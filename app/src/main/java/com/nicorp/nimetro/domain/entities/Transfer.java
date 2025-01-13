package com.nicorp.nimetro.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Transfer implements Parcelable {
    private List<Station> stations;
    private int time;
    private String type;
    private Map<String, String> transitionTexts; // Тексты переходов между станциями

    public Transfer(List<Station> stations, int time, String type) {
        this.stations = stations;
        this.time = time;
        this.type = type;
        this.transitionTexts = generateTransitionTexts();
    }

    protected Transfer(Parcel in) {
        stations = in.createTypedArrayList(Station.CREATOR);
        time = in.readInt();
        type = in.readString();
        transitionTexts = new HashMap<>();
        in.readMap(transitionTexts, String.class.getClassLoader());
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
        dest.writeMap(transitionTexts);
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

    @Override
    public String toString() {
        return "Transfer{" +
                "stations=" + stations +
                ", time=" + time +
                ", type='" + type + '\'' +
                '}';
    }
}