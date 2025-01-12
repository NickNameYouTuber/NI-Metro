package com.nicorp.nimetro.presentation.adapters;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Transfer;
import com.nicorp.nimetro.presentation.fragments.StationInfoFragment;

import java.util.List;

public class StationPagerAdapter extends FragmentStateAdapter {
    private final Station mainStation;
    private final Line mainLine;
    private final Station prevStation;
    private final Station nextStation;
    private final List<Transfer> transfers;
    private final List<Line> lines;
    private final List<Line> grayedLines;
    private final StationInfoFragment.OnStationInfoListener listener;

    public StationPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                               Station mainStation,
                               Line mainLine,
                               Station prevStation,
                               Station nextStation,
                               List<Transfer> transfers,
                               List<Line> lines,
                               List<Line> grayedLines,
                               StationInfoFragment.OnStationInfoListener listener) {
        super(fragmentActivity);
        this.mainStation = mainStation;
        this.mainLine = mainLine;
        this.prevStation = prevStation;
        this.nextStation = nextStation;
        this.transfers = transfers;
        this.lines = lines;
        this.grayedLines = grayedLines;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            // Основная станция
            StationInfoFragment fragment = StationInfoFragment.newInstance(
                    mainLine, mainStation, prevStation, nextStation, transfers, lines, grayedLines);
            fragment.setOnStationInfoListener(listener);
            return fragment;
        } else {
            // Переходы
            Transfer transfer = getTransferForStation(mainStation, position - 1);
            if (transfer != null) {
                List<Station> transferStations = transfer.getStations();
                // Находим станцию перехода, которая не является основной
                Station transferStation = null;
                int stationIndex = 0;
                for (Station station : transferStations) {
                    if (!station.equals(mainStation)) {
                        if (stationIndex == (position - 1)) {
                            transferStation = station;
                            break;
                        }
                        stationIndex++;
                    }
                }
                if (transferStation != null) {
                    Line transferLine = findLineForStation(transferStation);
                    Station transferPrev = findPrevStation(transferLine, transferStation);
                    Station transferNext = findNextStation(transferLine, transferStation);

                    StationInfoFragment fragment = StationInfoFragment.newInstance(
                            transferLine, transferStation, transferPrev, transferNext,
                            transfers, lines, grayedLines);
                    fragment.setOnStationInfoListener(listener);
                    return fragment;
                }
            }
            return new Fragment(); // Fallback
        }
    }

    @Override
    public int getItemCount() {
        int transferCount = getTransferCount(mainStation);
        return 1 + transferCount;
    }

    private int getTransferCount(Station station) {
        int count = 0;
        for (Transfer transfer : transfers) {
            // Получаем список станций в текущем переходе
            List<Station> transferStations = transfer.getStations();
            // Если выбранная станция есть в этом переходе
            if (transferStations.contains(station)) {
                // Увеличиваем счётчик на количество станций в переходе, минус выбранная станция
                count += transferStations.size() - 1;
            }
        }
        return count;
    }

    private Transfer getTransferForStation(Station station, int index) {
        int currentIndex = 0;
        for (Transfer transfer : transfers) {
            List<Station> transferStations = transfer.getStations();
            if (transferStations.contains(station)) {
                // Количество переходов в текущем Transfer
                int transferCount = transferStations.size() - 1;
                if (currentIndex + transferCount > index) {
                    // Возвращаем текущий Transfer и индекс станции в нём
                    return transfer;
                }
                currentIndex += transferCount;
            }
        }
        return null;
    }

    private Station getOtherStation(Transfer transfer, Station currentStation) {
        List<Station> transferStations = transfer.getStations();
        for (Station station : transferStations) {
            if (!station.equals(currentStation)) {
                return station;
            }
        }
        return null;
    }

    private Line findLineForStation(Station station) {
        for (Line line : lines) {
            if (line.getStations().contains(station)) {
                return line;
            }
        }
        for (Line line : grayedLines) {
            if (line.getStations().contains(station)) {
                return line;
            }
        }
        return null;
    }

    private Station findPrevStation(Line line, Station station) {
        List<Station> stations = line.getStations();
        int index = stations.indexOf(station);
        if (index > 0) {
            return stations.get(index - 1);
        }
        return null;
    }

    private Station findNextStation(Line line, Station station) {
        List<Station> stations = line.getStations();
        int index = stations.indexOf(station);
        if (index < stations.size() - 1) {
            return stations.get(index + 1);
        }
        return null;
    }
}