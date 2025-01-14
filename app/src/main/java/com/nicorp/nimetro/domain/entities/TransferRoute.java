package com.nicorp.nimetro.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class TransferRoute implements Parcelable {
    private String transferMap;
    private String from;
    private String to;
    private List<String> way;
    private String next;
    private String prev; // Добавляем поле для предыдущей станции

    public TransferRoute(String transferMap, String from, String to, List<String> way, String prev, String next) {
        this.transferMap = transferMap;
        this.from = from;
        this.to = to;
        this.way = way;
        this.prev = prev; // Инициализируем предыдущую станцию
        this.next = next;
    }

    protected TransferRoute(Parcel in) {
        transferMap = in.readString();
        from = in.readString();
        to = in.readString();
        way = in.createStringArrayList();
        prev = in.readString(); // Читаем предыдущую станцию из Parcel
        next = in.readString();
    }

    public static final Creator<TransferRoute> CREATOR = new Creator<TransferRoute>() {
        @Override
        public TransferRoute createFromParcel(Parcel in) {
            return new TransferRoute(in);
        }

        @Override
        public TransferRoute[] newArray(int size) {
            return new TransferRoute[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(transferMap);
        dest.writeString(from);
        dest.writeString(to);
        dest.writeStringList(way);
        dest.writeString(prev); // Записываем предыдущую станцию в Parcel
        dest.writeString(next);
    }

    // Геттеры и сеттеры
    public String getTransferMap() {
        return transferMap;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public List<String> getWay() {
        return way;
    }

    public String getPrev() {
        return prev;
    }

    public String getNext() {
        return next;
    }

    public void setPrev(String prev) {
        this.prev = prev;
    }
}