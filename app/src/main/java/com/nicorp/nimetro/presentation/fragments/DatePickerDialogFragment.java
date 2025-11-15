package com.nicorp.nimetro.presentation.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class DatePickerDialogFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private DatePickerDialog.OnDateSetListener listener;

    public static DatePickerDialogFragment newInstance(DatePickerDialog.OnDateSetListener listener) {
        DatePickerDialogFragment fragment = new DatePickerDialogFragment();
        fragment.listener = listener;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        Context context = getActivity();
        if (context == null) {
            return new DatePickerDialog(getContext());
        }
        int themeResId = com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar;
        return new DatePickerDialog(context, themeResId, this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        if (listener != null) {
            listener.onDateSet(view, year, month, dayOfMonth);
        }
    }
}