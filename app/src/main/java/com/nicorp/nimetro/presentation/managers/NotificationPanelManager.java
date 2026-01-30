package com.nicorp.nimetro.presentation.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.Notification;
import com.nicorp.nimetro.domain.entities.NotificationTrigger;
import com.nicorp.nimetro.domain.entities.StationNotification;
import com.nicorp.nimetro.data.services.NotificationSyncService;
import com.nicorp.nimetro.data.exceptions.ApiException;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationPanelManager {
    private static final String TAG = "NotificationPanelManager";
    private static final String PREFS_NAME = "notification_prefs";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private Context context;
    private LinearLayout notificationPanel;
    private ImageView closeButton;
    private List<Notification> notifications;
    private SharedPreferences sharedPreferences;
    private NotificationSyncService notificationSyncService;

    public NotificationPanelManager(Context context, LinearLayout notificationPanel, ImageView closeButton) {
        this.context = context;
        this.notificationPanel = notificationPanel;
        this.closeButton = closeButton;
        this.notifications = new ArrayList<>();
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.notificationSyncService = new NotificationSyncService(context);
        setupCloseButton();
    }

    private void setupCloseButton() {
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> hideNotificationPanel());
        }
    }

    public void loadNotifications() {
        new Thread(() -> {
            try {
                JSONArray generalNotifications = null;

                if (notificationSyncService.isOnline()) {
                    try {
                        generalNotifications = notificationSyncService.syncNotifications();
                        Log.d(TAG, "Notifications loaded from API");
                    } catch (ApiException e) {
                        Log.w(TAG, "Failed to load notifications from API, trying cache", e);
                        generalNotifications = notificationSyncService.getNotificationsFromCache();
                    }
                } else {
                    Log.d(TAG, "No internet connection, loading from cache");
                    generalNotifications = notificationSyncService.getNotificationsFromCache();
                }

                if (generalNotifications == null || generalNotifications.length() == 0) {
                    Log.d(TAG, "No cached notifications, loading from assets");
                    generalNotifications = loadNotificationsFromAssets();
                }

                if (generalNotifications != null) {
                    notifications.clear();
                    for (int i = 0; i < generalNotifications.length(); i++) {
                        JSONObject notificationObj = generalNotifications.getJSONObject(i);
                        Notification notification = parseNotification(notificationObj);
                        if (notification != null) {
                            notifications.add(notification);
                        }
                    }
                    Log.d(TAG, "Loaded " + notifications.size() + " notifications");
                    
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            checkAndShowNotifications();
                        });
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error loading notifications", e);
            }
        }).start();
    }

    private JSONArray loadNotificationsFromAssets() {
        try {
            String json = loadJSONFromAsset("notifications.json");
            if (json == null) {
                Log.w(TAG, "Failed to load notifications.json from assets");
                return null;
            }

            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.optJSONArray("general_notifications");
        } catch (JSONException e) {
            Log.e(TAG, "Error loading notifications from assets", e);
            return null;
        }
    }

    private Notification parseNotification(JSONObject jsonObject) throws JSONException {
        String id = jsonObject.getString("id");
        String typeStr = jsonObject.getString("type");
        Notification.NotificationType type = "important".equals(typeStr) 
            ? Notification.NotificationType.IMPORTANT 
            : Notification.NotificationType.NORMAL;

        JSONObject triggerObj = jsonObject.getJSONObject("trigger");
        NotificationTrigger trigger = parseTrigger(triggerObj);

        JSONObject contentObj = jsonObject.getJSONObject("content");
        Notification.NotificationContent content = parseContent(contentObj);

        return new Notification(id, type, trigger, content);
    }

    private NotificationTrigger parseTrigger(JSONObject jsonObject) throws JSONException {
        String typeStr = jsonObject.getString("type");
        
        if ("station".equals(typeStr)) {
            String stationId = jsonObject.getString("station_id");
            NotificationTrigger trigger = new NotificationTrigger(NotificationTrigger.TriggerType.STATION, stationId);
            
            if (jsonObject.has("date_range")) {
                JSONObject dateRangeObj = jsonObject.getJSONObject("date_range");
                String start = dateRangeObj.getString("start");
                String end = dateRangeObj.getString("end");
                NotificationTrigger.DateRange dateRange = new NotificationTrigger.DateRange(start, end);
                trigger.setDateRange(dateRange);
            }
            
            return trigger;
        } else if ("line".equals(typeStr)) {
            String lineId = jsonObject.getString("line_id");
            NotificationTrigger.DateRange dateRange = null;
            
            if (jsonObject.has("date_range")) {
                JSONObject dateRangeObj = jsonObject.getJSONObject("date_range");
                String start = dateRangeObj.getString("start");
                String end = dateRangeObj.getString("end");
                dateRange = new NotificationTrigger.DateRange(start, end);
            }
            
            return new NotificationTrigger(NotificationTrigger.TriggerType.LINE, lineId, dateRange);
        } else if ("date_range".equals(typeStr)) {
            JSONObject dateRangeObj = jsonObject.getJSONObject("date_range");
            String start = dateRangeObj.getString("start");
            String end = dateRangeObj.getString("end");
            NotificationTrigger.DateRange dateRange = new NotificationTrigger.DateRange(start, end);
            return new NotificationTrigger(NotificationTrigger.TriggerType.DATE_RANGE, dateRange);
        } else {
            return new NotificationTrigger(NotificationTrigger.TriggerType.ONCE, (NotificationTrigger.DateRange) null);
        }
    }

    private Notification.NotificationContent parseContent(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("image_url") || jsonObject.has("image_resource")) {
            String imageUrl = jsonObject.optString("image_url", null);
            String imageResource = jsonObject.optString("image_resource", null);
            String caption = jsonObject.optString("caption", null);
            Notification.NotificationContent content = new Notification.NotificationContent();
            content.setImageUrl(imageUrl);
            content.setImageResource(imageResource);
            content.setCaption(caption);
            return content;
        } else {
            String text = jsonObject.getString("text");
            return new Notification.NotificationContent(text);
        }
    }

    public void checkAndShowNotifications() {
        notificationPanel.removeAllViews();
        boolean hasVisibleNotifications = false;

        for (Notification notification : notifications) {
            if (shouldShowNotification(notification)) {
                addNotificationView(notification);
                hasVisibleNotifications = true;
            }
        }

        if (hasVisibleNotifications) {
            showNotificationPanel();
        } else {
            hideNotificationPanel();
        }
    }

    private boolean shouldShowNotification(Notification notification) {
        NotificationTrigger trigger = notification.getTrigger();
        if (trigger == null) {
            return false;
        }

        if (trigger.getType() == NotificationTrigger.TriggerType.STATION) {
            return false;
        }

        if (trigger.getType() == NotificationTrigger.TriggerType.LINE) {
            return false;
        }

        if (trigger.getType() == NotificationTrigger.TriggerType.ONCE) {
            String key = "notification_shown_" + notification.getId();
            return !sharedPreferences.getBoolean(key, false);
        } else if (trigger.getType() == NotificationTrigger.TriggerType.DATE_RANGE) {
            NotificationTrigger.DateRange dateRange = trigger.getDateRange();
            if (dateRange == null) {
                return false;
            }
            return isDateInRange(dateRange.getStart(), dateRange.getEnd());
        }

        return false;
    }

    public Notification getStationNotification(String stationId, List<String> lineIds) {
        if (stationId == null) {
            return null;
        }
        
        for (Notification notification : notifications) {
            NotificationTrigger trigger = notification.getTrigger();
            if (trigger == null) {
                continue;
            }
            
            if (trigger.getType() == NotificationTrigger.TriggerType.STATION &&
                stationId.equals(trigger.getStationId())) {
                
                NotificationTrigger.DateRange dateRange = trigger.getDateRange();
                if (dateRange != null) {
                    if (!isDateInRange(dateRange.getStart(), dateRange.getEnd())) {
                        continue;
                    }
                }
                
                return notification;
            } else if (trigger.getType() == NotificationTrigger.TriggerType.LINE && 
                       lineIds != null && lineIds.contains(trigger.getLineId())) {
                
                NotificationTrigger.DateRange dateRange = trigger.getDateRange();
                if (dateRange != null) {
                    if (!isDateInRange(dateRange.getStart(), dateRange.getEnd())) {
                        continue;
                    }
                } else {
                    continue;
                }
                
                return notification;
            }
        }
        
        return null;
    }

    public Notification getStationNotification(String stationId) {
        return getStationNotification(stationId, null);
    }

    public Map<String, StationNotification> getAllStationNotifications() {
        Map<String, StationNotification> stationNotificationsMap = new HashMap<>();
        
        for (Notification notification : notifications) {
            NotificationTrigger trigger = notification.getTrigger();
            if (trigger != null && trigger.getType() == NotificationTrigger.TriggerType.STATION) {
                String stationId = trigger.getStationId();
                if (stationId != null) {
                    NotificationTrigger.DateRange dateRange = trigger.getDateRange();
                    if (dateRange != null) {
                        if (!isDateInRange(dateRange.getStart(), dateRange.getEnd())) {
                            continue;
                        }
                    }
                    
                    StationNotification.NotificationType type = 
                        notification.getType() == Notification.NotificationType.IMPORTANT
                            ? StationNotification.NotificationType.IMPORTANT
                            : StationNotification.NotificationType.NORMAL;
                    
                    String text = notification.getContent() != null 
                        ? notification.getContent().getText() 
                        : null;
                    
                    if (text != null) {
                        stationNotificationsMap.put(stationId, new StationNotification(type, text));
                    }
                }
            }
        }
        
        return stationNotificationsMap;
    }

    private boolean isDateInRange(String startStr, String endStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            Date startDate = sdf.parse(startStr);
            Date endDate = sdf.parse(endStr);
            Date currentDate = new Date();

            String currentDateStr = sdf.format(currentDate);
            Date currentDateParsed = sdf.parse(currentDateStr);

            return !currentDateParsed.before(startDate) && !currentDateParsed.after(endDate);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date", e);
            return false;
        }
    }

    private void addNotificationView(Notification notification) {
        Notification.NotificationContent content = notification.getContent();
        
        if (content != null && content.hasImage()) {
            addImageNotificationView(notification);
        } else {
            addTextNotificationView(notification);
        }
    }

    private void addTextNotificationView(Notification notification) {
        View notificationView = createNotificationView(notification);
        notificationPanel.addView(notificationView);
    }

    private void addImageNotificationView(Notification notification) {
        View notificationView = createNotificationView(notification);
        notificationPanel.addView(notificationView);
    }

    public void addCustomView(View view) {
        if (view != null) {
            notificationPanel.addView(view);
            showNotificationPanel();
        }
    }

    public void addImageNotification(String imageUrl, String imageResource, String caption, boolean isImportant) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View notificationView = inflater.inflate(R.layout.item_notification_image, notificationPanel, false);

        ImageView imageView = notificationView.findViewById(R.id.notificationImage);
        TextView captionView = notificationView.findViewById(R.id.notificationCaption);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                .load(imageUrl)
                .into(imageView);
        } else if (imageResource != null && !imageResource.isEmpty()) {
            int resourceId = context.getResources().getIdentifier(
                imageResource, 
                "drawable", 
                context.getPackageName()
            );
            if (resourceId != 0) {
                Glide.with(context)
                    .load(resourceId)
                    .into(imageView);
            }
        }

        if (caption != null && !caption.isEmpty()) {
            captionView.setText(caption);
            captionView.setVisibility(View.VISIBLE);
        } else {
            captionView.setVisibility(View.GONE);
        }

        if (isImportant) {
            notificationView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light, null));
            captionView.setTextColor(context.getResources().getColor(android.R.color.black, null));
        } else {
            notificationView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent, null));
        }

        ImageView itemCloseButton = notificationView.findViewById(R.id.notificationItemCloseButton);
        itemCloseButton.setOnClickListener(v -> {
            notificationPanel.removeView(notificationView);
            if (notificationPanel.getChildCount() == 0) {
                hideNotificationPanel();
            }
        });

        notificationPanel.addView(notificationView);
        showNotificationPanel();
    }

    public void showStationNotification(Notification notification, String stationId) {
        if (notification == null) {
            return;
        }

        notificationPanel.addView(createNotificationView(notification), 0);
        showNotificationPanel();
    }

    private View createNotificationView(Notification notification) {
        Notification.NotificationContent content = notification.getContent();
        
        LayoutInflater inflater = LayoutInflater.from(context);
        View notificationView;
        
        if (content != null && content.hasImage()) {
            notificationView = inflater.inflate(R.layout.item_notification_image, notificationPanel, false);
            setupImageNotificationView(notificationView, notification);
        } else {
            notificationView = inflater.inflate(R.layout.item_notification, notificationPanel, false);
            setupTextNotificationView(notificationView, notification);
        }

        ImageView itemCloseButton = notificationView.findViewById(R.id.notificationItemCloseButton);
        itemCloseButton.setOnClickListener(v -> {
            if (notification.getTrigger().getType() == NotificationTrigger.TriggerType.STATION) {
                notificationPanel.removeView(notificationView);
                if (notificationPanel.getChildCount() == 0) {
                    hideNotificationPanel();
                }
            } else {
                hideNotification(notification.getId());
                notificationPanel.removeView(notificationView);
                if (notificationPanel.getChildCount() == 0) {
                    hideNotificationPanel();
                }
            }
        });

        return notificationView;
    }

    private void setupTextNotificationView(View notificationView, Notification notification) {
        TextView textView = notificationView.findViewById(R.id.notificationText);
        textView.setText(notification.getContent().getText());

        if (notification.getType() == Notification.NotificationType.IMPORTANT) {
            notificationView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light, null));
            textView.setTextColor(context.getResources().getColor(android.R.color.black, null));
        } else {
            notificationView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent, null));
        }
    }

    private void setupImageNotificationView(View notificationView, Notification notification) {
        ImageView imageView = notificationView.findViewById(R.id.notificationImage);
        TextView captionView = notificationView.findViewById(R.id.notificationCaption);
        Notification.NotificationContent content = notification.getContent();

        if (content.getImageUrl() != null && !content.getImageUrl().isEmpty()) {
            Glide.with(context)
                .load(content.getImageUrl())
                .into(imageView);
        } else if (content.getImageResource() != null && !content.getImageResource().isEmpty()) {
            int resourceId = context.getResources().getIdentifier(
                content.getImageResource(), 
                "drawable", 
                context.getPackageName()
            );
            if (resourceId != 0) {
                Glide.with(context)
                    .load(resourceId)
                    .into(imageView);
            }
        }

        if (content.getCaption() != null && !content.getCaption().isEmpty()) {
            captionView.setText(content.getCaption());
            captionView.setVisibility(View.VISIBLE);
        } else {
            captionView.setVisibility(View.GONE);
        }

        if (notification.getType() == Notification.NotificationType.IMPORTANT) {
            notificationView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light, null));
            captionView.setTextColor(context.getResources().getColor(android.R.color.black, null));
        } else {
            notificationView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent, null));
        }
    }

    public void clearStationNotifications() {
        if (notificationPanel == null) {
            return;
        }

        List<Notification> generalNotificationsToKeep = new ArrayList<>();
        for (Notification notification : notifications) {
            if (shouldShowNotification(notification)) {
                generalNotificationsToKeep.add(notification);
            }
        }

        notificationPanel.removeAllViews();

        for (Notification notification : generalNotificationsToKeep) {
            addNotificationView(notification);
        }

        if (notificationPanel.getChildCount() == 0) {
            hideNotificationPanel();
        } else {
            showNotificationPanel();
        }
    }

    public void hideNotification(String notificationId) {
        if (notificationId == null) {
            return;
        }

        Notification notification = findNotificationById(notificationId);
        if (notification != null && notification.getTrigger().getType() == NotificationTrigger.TriggerType.ONCE) {
            markAsShown(notificationId);
        }
    }

    private Notification findNotificationById(String id) {
        for (Notification notification : notifications) {
            if (TextUtils.equals(notification.getId(), id)) {
                return notification;
            }
        }
        return null;
    }

    public void markAsShown(String notificationId) {
        if (notificationId != null) {
            sharedPreferences.edit().putBoolean("notification_shown_" + notificationId, true).apply();
        }
    }

    private void showNotificationPanel() {
        if (notificationPanel != null) {
            notificationPanel.setVisibility(View.VISIBLE);
        }
    }

    private void hideNotificationPanel() {
        if (notificationPanel != null) {
            notificationPanel.setVisibility(View.GONE);
        }
    }

    public void setPositionAboveStationPager(boolean aboveStationPager) {
        if (notificationPanel == null || notificationPanel.getLayoutParams() == null) {
            return;
        }

        if (!(notificationPanel.getLayoutParams() instanceof ConstraintLayout.LayoutParams)) {
            return;
        }

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) notificationPanel.getLayoutParams();
        
        if (aboveStationPager) {
            params.bottomToTop = R.id.stationPager;
            params.topToTop = ConstraintLayout.LayoutParams.UNSET;
        } else {
            params.bottomToTop = R.id.linearLayout2;
            params.topToTop = ConstraintLayout.LayoutParams.UNSET;
        }
        
        notificationPanel.setLayoutParams(params);
    }

    private String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("raw/" + fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.e(TAG, "Error loading JSON from assets", ex);
            return null;
        }
        return json;
    }

    public void startPeriodicSync() {
        new Thread(() -> {
            try {
                if (notificationSyncService.isOnline()) {
                    notificationSyncService.syncNotifications();
                    Log.d(TAG, "Periodic notification sync completed");
                }
            } catch (ApiException e) {
                Log.e(TAG, "Periodic notification sync failed", e);
            }
        }).start();
    }
}

