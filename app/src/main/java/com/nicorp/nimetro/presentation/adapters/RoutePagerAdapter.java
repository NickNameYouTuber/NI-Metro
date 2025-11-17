package com.nicorp.nimetro.presentation.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.RouteStation;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.presentation.activities.MainActivity;
import com.nicorp.nimetro.presentation.fragments.RouteInfoFragment;
import com.nicorp.nimetro.presentation.views.MetroMapView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoutePagerAdapter extends FragmentStateAdapter {
    public enum RouteType {
        FASTEST,
        FEW_TRANSFERS
    }

    private final List<RouteStation> fastestRoute;
    private final List<RouteStation> fewTransfersRoute;
    private final MetroMapView metroMapView;
    private final MainActivity mainActivity;

    public RoutePagerAdapter(@NonNull FragmentActivity fragmentActivity,
                             List<RouteStation> fastestRoute,
                             List<RouteStation> fewTransfersRoute,
                             MetroMapView metroMapView,
                             MainActivity mainActivity) {
        super(fragmentActivity);
        this.fastestRoute = fastestRoute;
        this.fewTransfersRoute = fewTransfersRoute;
        this.metroMapView = metroMapView;
        this.mainActivity = mainActivity;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        List<Station> fastestStationRoute = convertRouteStationsToStations(fastestRoute);
        List<Station> fewTransfersStationRoute = convertRouteStationsToStations(fewTransfersRoute);
        RouteInfoFragment fragment;
        if (position == 0) {
            fragment = RouteInfoFragment.newInstanceWithVariants(
                    fastestStationRoute, fewTransfersStationRoute, metroMapView, mainActivity, RouteType.FASTEST);
            Map<Station, Line> routeLineMap = createRouteLineMap(fastestRoute);
            fragment.setRouteLineMap(routeLineMap);
        } else {
            fragment = RouteInfoFragment.newInstanceWithVariants(
                    fastestStationRoute, fewTransfersStationRoute, metroMapView, mainActivity, RouteType.FEW_TRANSFERS);
            Map<Station, Line> routeLineMap = createRouteLineMap(fewTransfersRoute);
            fragment.setRouteLineMap(routeLineMap);
        }
        return fragment;
    }
    
    private Map<Station, Line> createRouteLineMap(List<RouteStation> routeStations) {
        Map<Station, Line> routeLineMap = new HashMap<>();
        if (routeStations != null) {
            for (RouteStation routeStation : routeStations) {
                if (routeStation != null && routeStation.getStation() != null && routeStation.getLine() != null) {
                    routeLineMap.put(routeStation.getStation(), routeStation.getLine());
                }
            }
        }
        return routeLineMap;
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public List<RouteStation> getRouteAtPosition(int position) {
        if (position == 0) {
            return fastestRoute;
        } else if (position == 1) {
            return fewTransfersRoute;
        }
        return null;
    }
    
    private List<Station> convertRouteStationsToStations(List<RouteStation> routeStations) {
        if (routeStations == null) {
            return new ArrayList<>();
        }
        List<Station> stations = new ArrayList<>();
        for (RouteStation routeStation : routeStations) {
            if (routeStation != null && routeStation.getStation() != null) {
                stations.add(routeStation.getStation());
            }
        }
        return stations;
    }

    public RouteType getRouteTypeAtPosition(int position) {
        if (position == 0) {
            return RouteType.FASTEST;
        } else if (position == 1) {
            return RouteType.FEW_TRANSFERS;
        }
        return RouteType.FASTEST;
    }
}

