package com.nicorp.nimetro.presentation.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.presentation.activities.MainActivity;
import com.nicorp.nimetro.presentation.fragments.RouteInfoFragment;
import com.nicorp.nimetro.presentation.views.MetroMapView;

import java.util.List;

public class RoutePagerAdapter extends FragmentStateAdapter {
    public enum RouteType {
        FASTEST,
        FEW_TRANSFERS
    }

    private final List<Station> fastestRoute;
    private final List<Station> fewTransfersRoute;
    private final MetroMapView metroMapView;
    private final MainActivity mainActivity;

    public RoutePagerAdapter(@NonNull FragmentActivity fragmentActivity,
                             List<Station> fastestRoute,
                             List<Station> fewTransfersRoute,
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
        if (position == 0) {
            return RouteInfoFragment.newInstanceWithVariants(
                    fastestRoute, fewTransfersRoute, metroMapView, mainActivity, RouteType.FASTEST);
        } else {
            return RouteInfoFragment.newInstanceWithVariants(
                    fastestRoute, fewTransfersRoute, metroMapView, mainActivity, RouteType.FEW_TRANSFERS);
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public List<Station> getRouteAtPosition(int position) {
        if (position == 0) {
            return fastestRoute;
        } else if (position == 1) {
            return fewTransfersRoute;
        }
        return null;
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

