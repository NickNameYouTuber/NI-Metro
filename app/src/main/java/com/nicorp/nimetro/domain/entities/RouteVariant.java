package com.nicorp.nimetro.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;

public class RouteVariant implements Parcelable {
    private final Route route;
    private final RouteVariantType variantType;

    public RouteVariant(Route route, RouteVariantType variantType) {
        this.route = route;
        this.variantType = variantType;
    }

    protected RouteVariant(Parcel in) {
        route = in.readParcelable(Route.class.getClassLoader());
        String typeName = in.readString();
        if (typeName == null) {
            variantType = RouteVariantType.FASTEST;
        } else {
            variantType = RouteVariantType.valueOf(typeName);
        }
    }

    public static final Creator<RouteVariant> CREATOR = new Creator<RouteVariant>() {
        @Override
        public RouteVariant createFromParcel(Parcel in) {
            return new RouteVariant(in);
        }

        @Override
        public RouteVariant[] newArray(int size) {
            return new RouteVariant[size];
        }
    };

    public Route getRoute() {
        return route;
    }

    public RouteVariantType getVariantType() {
        return variantType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(route, flags);
        dest.writeString(variantType.name());
    }
}


