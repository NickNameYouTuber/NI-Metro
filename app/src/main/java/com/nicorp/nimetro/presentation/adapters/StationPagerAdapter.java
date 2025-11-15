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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StationPagerAdapter extends FragmentStateAdapter {
    private final Station mainStation;
    private final Line mainLine;
    private final Station prevStation;
    private final Station nextStation;
    private final List<Transfer> transfers;
    private final List<Line> lines;
    private final List<Line> grayedLines;
    private final StationInfoFragment.OnStationInfoListener listener;
    private final List<TransferStationEntry> transferStationEntries;

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
        this.transferStationEntries = buildTransferStationEntries(mainStation, transfers);
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
        }

        TransferStationEntry entry = getTransferStationEntry(position - 1);
        if (entry != null) {
            Station transferStation = entry.station;
            Line transferLine = findLineForStation(transferStation);
            Station transferPrev = findPrevStation(transferLine, transferStation);
            Station transferNext = findNextStation(transferLine, transferStation);

            StationInfoFragment fragment = StationInfoFragment.newInstance(
                    transferLine, transferStation, transferPrev, transferNext,
                    transfers, lines, grayedLines);
            fragment.setOnStationInfoListener(listener);
            return fragment;
        }
        return new Fragment(); // Fallback
    }

    @Override
    public int getItemCount() {
        return 1 + transferStationEntries.size();
    }

    public Station getStationAtPosition(int position) {
        if (position == 0) {
            return mainStation;
        }
        TransferStationEntry entry = getTransferStationEntry(position - 1);
        return entry != null ? entry.station : null;
    }

    public Line getLineAtPosition(int position) {
        if (position == 0) {
            return mainLine;
        }
        TransferStationEntry entry = getTransferStationEntry(position - 1);
        if (entry != null) {
            return findLineForStation(entry.station);
        }
        return null;
    }

    private TransferStationEntry getTransferStationEntry(int index) {
        if (index < 0 || index >= transferStationEntries.size()) {
            return null;
        }
        return transferStationEntries.get(index);
    }

    private List<TransferStationEntry> buildTransferStationEntries(Station baseStation, List<Transfer> transfers) {
        List<TransferStationEntry> entries = new ArrayList<>();
        if (baseStation == null || transfers == null) {
            return entries;
        }
        Set<String> seenKeys = new LinkedHashSet<>();
        for (Transfer transfer : transfers) {
            if (transfer == null) {
                continue;
            }
            List<Station> transferStations = transfer.getStations();
            if (transferStations == null || !transferStations.contains(baseStation)) {
                continue;
            }
            for (Station candidate : transferStations) {
                if (candidate == null || candidate.equals(baseStation)) {
                    continue;
                }
                String name = candidate.getName();
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                String stationId = candidate.getId();
                String key = (stationId != null && !stationId.trim().isEmpty()) ? stationId.trim() : name.trim();
                if (!seenKeys.add(key)) {
                    continue;
                }
                entries.add(new TransferStationEntry(transfer, candidate));
            }
        }
        return entries;
    }

    private static class TransferStationEntry {
        final Transfer transfer;
        final Station station;

        TransferStationEntry(Transfer transfer, Station station) {
            this.transfer = transfer;
            this.station = station;
        }
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