package com.nicorp.nimetro.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Transfer implements Parcelable {
    private String id;
    private List<Station> stations;
    private int time;
    private String type;
    private String transferMap;
    private Map<String, String> transitionTexts; // Тексты переходов между станциями
    private List<TransferRoute> transferRoutes; // Маршруты перехода
    private boolean isLinkTransfer; // Переход между другими переходами (TR_* ссылки)
    private List<String> linkedTransferIds; // ID переходов, которые соединяет этот переход
    private List<String> linkedStationIds; // Явно указанные станции-анкеры, к которым тянем от центров переходов

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

    public Transfer(String id, List<Station> stations, int time, String type, String transferMap, List<TransferRoute> transferRoutes) {
        this.id = id;
        this.stations = stations;
        this.time = time;
        this.type = type;
        this.transferMap = transferMap;
        this.transferRoutes = transferRoutes;
        this.transitionTexts = generateTransitionTexts();
    }

    public Transfer(String id, List<Station> stations, int time, String type, String transferMap, List<TransferRoute> transferRoutes, boolean isLinkTransfer, List<String> linkedTransferIds) {
        this.id = id;
        this.stations = stations;
        this.time = time;
        this.type = type;
        this.transferMap = transferMap;
        this.transferRoutes = transferRoutes;
        this.isLinkTransfer = isLinkTransfer;
        this.linkedTransferIds = linkedTransferIds;
        this.transitionTexts = generateTransitionTexts();
    }

    public Transfer(String id, List<Station> stations, int time, String type, String transferMap, List<TransferRoute> transferRoutes, boolean isLinkTransfer, List<String> linkedTransferIds, List<String> linkedStationIds) {
        this.id = id;
        this.stations = stations;
        this.time = time;
        this.type = type;
        this.transferMap = transferMap;
        this.transferRoutes = transferRoutes;
        this.isLinkTransfer = isLinkTransfer;
        this.linkedTransferIds = linkedTransferIds;
        this.linkedStationIds = linkedStationIds;
        this.transitionTexts = generateTransitionTexts();
    }

    protected Transfer(Parcel in) {
        id = in.readString();
        stations = in.createTypedArrayList(Station.CREATOR);
        time = in.readInt();
        type = in.readString();
        transferMap = in.readString();
        transitionTexts = new HashMap<>();
        in.readMap(transitionTexts, String.class.getClassLoader());
        transferRoutes = in.createTypedArrayList(TransferRoute.CREATOR);
        isLinkTransfer = in.readByte() != 0;
        linkedTransferIds = in.createStringArrayList();
        linkedStationIds = in.createStringArrayList();
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
        dest.writeString(id);
        dest.writeTypedList(stations);
        dest.writeInt(time);
        dest.writeString(type);
        dest.writeString(transferMap);
        dest.writeMap(transitionTexts);
        dest.writeTypedList(transferRoutes);
        dest.writeByte((byte) (isLinkTransfer ? 1 : 0));
        dest.writeStringList(linkedTransferIds);
        dest.writeStringList(linkedStationIds);
    }

    public List<Station> getStations() {
        return stations;
    }

    public String getId() {
        return id;
    }

    public boolean isLinkTransfer() {
        return isLinkTransfer;
    }

    public List<String> getLinkedTransferIds() {
        return linkedTransferIds;
    }

    public List<String> getLinkedStationIds() {
        return linkedStationIds;
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
        Log.d("Transfer", "getTransferRoute called with prev=" + (prev != null ? prev.getId() : "null") + 
              ", from=" + from.getId() + ", to=" + to.getId());
        Log.d("Transfer", "Transfer has " + (transferRoutes != null ? transferRoutes.size() : 0) + " routes");
        
        if (transferRoutes == null || transferRoutes.isEmpty()) {
            Log.d("Transfer", "No transfer routes available");
            return null;
        }
        
        TransferRoute fallbackAnyFromTo = null;
        for (int i = 0; i < transferRoutes.size(); i++) {
            TransferRoute route = transferRoutes.get(i);
            Log.d("Transfer", "Route " + i + ": from=" + route.getFrom() + ", to=" + route.getTo() + ", prev=" + route.getPrev());
            
            // Проверяем, подходит ли маршрут для текущего перехода по from/to
            if (route.getFrom() != null && route.getTo() != null
                    && route.getFrom().equals(from.getId()) && route.getTo().equals(to.getId())) {
                Log.d("Transfer", "Route " + i + " matches from/to");
                
                // Сначала пробуем точное совпадение по prev
                if (prev != null && route.getPrev() != null && route.getPrev().equals(prev.getId())) {
                    Log.d("Transfer", "Route " + i + " matches prev - EXACT MATCH!");
                    return route;
                }
                // Если у маршрута prev не задан — используем его как предпочтительный
                if (route.getPrev() == null) {
                    Log.d("Transfer", "Route " + i + " has no prev - PREFERRED MATCH!");
                    return route;
                }
                // Запоминаем любой подходящий по from/to маршрут как запасной
                if (fallbackAnyFromTo == null) {
                    Log.d("Transfer", "Route " + i + " saved as fallback");
                    fallbackAnyFromTo = route;
                }
            } else {
                Log.d("Transfer", "Route " + i + " does NOT match from/to");
            }
        }
        // Фоллбек: если точного совпадения по prev нет — вернём любой подходящий по from/to
        if (fallbackAnyFromTo != null) {
            Log.d("Transfer", "Returning fallback route");
        } else {
            Log.d("Transfer", "No matching route found");
        }
        return fallbackAnyFromTo;
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