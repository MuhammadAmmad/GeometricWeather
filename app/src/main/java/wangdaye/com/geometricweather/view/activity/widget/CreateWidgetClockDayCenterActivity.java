package wangdaye.com.geometricweather.view.activity.widget;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import wangdaye.com.geometricweather.data.entity.model.Location;
import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.data.entity.model.Weather;
import wangdaye.com.geometricweather.service.widget.alarm.WidgetClockDayCenterAlarmService;
import wangdaye.com.geometricweather.utils.WidgetUtils;
import wangdaye.com.geometricweather.utils.helpter.DatabaseHelper;
import wangdaye.com.geometricweather.utils.helpter.LocationHelper;
import wangdaye.com.geometricweather.utils.SnackbarUtils;
import wangdaye.com.geometricweather.utils.TimeUtils;
import wangdaye.com.geometricweather.utils.helpter.WeatherHelper;
import wangdaye.com.geometricweather.basic.GeoActivity;

/**
 * Create widget clock day center activity.
 * */

public class CreateWidgetClockDayCenterActivity extends GeoActivity
        implements View.OnClickListener, AdapterView.OnItemSelectedListener,
        WeatherHelper.OnRequestWeatherListener, LocationHelper.OnRequestLocationListener {
    // widget
    private ImageView widgetCard;
    private ImageView widgetIcon;
    private TextClock widgetClock;
    private TextView widgetWeather;
    private TextView widgetTemp;
    private TextView widgetRefreshTime;

    private CoordinatorLayout container;

    private Switch showCardSwitch;
    private Switch blackTextSwitch;
    private Switch hideRefreshTimeSwitch;

    // data
    private Location location;
    private List<String> nameList;

    private WeatherHelper weatherHelper;
    private LocationHelper locationHelper;

    /** <br> life cycle. */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_widget_clock_day_center);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isStarted()) {
            setStarted();
            initData();
            initWidget();

            if (location.name.equals(getString(R.string.local))) {
                locationHelper.requestLocation(this, this);
            } else {
                if (location.name.equals(getString(R.string.local))) {
                    location.realName = location.name;
                    DatabaseHelper.getInstance(this).insertLocation(location);
                }
                weatherHelper.requestWeather(this, location, this);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        weatherHelper.cancel();
        locationHelper.cancel();
    }

    @Override
    public View getSnackbarContainer() {
        return container;
    }

    /** <br> UI. */

    private void initWidget() {
        this.widgetCard = (ImageView) findViewById(R.id.widget_clock_day_center_card);
        widgetCard.setVisibility(View.GONE);

        this.widgetIcon = (ImageView) findViewById(R.id.widget_clock_day_center_icon);
        this.widgetClock = (TextClock) findViewById(R.id.widget_clock_day_center_clock);
        this.widgetWeather = (TextView) findViewById(R.id.widget_clock_day_center_weather);
        this.widgetTemp = (TextView) findViewById(R.id.widget_clock_day_center_temp);
        this.widgetRefreshTime = (TextView) findViewById(R.id.widget_clock_day_center_refreshTime);

        ImageView wallpaper = (ImageView) findViewById(R.id.activity_create_widget_clock_day_center_wall);
        wallpaper.setImageDrawable(WallpaperManager.getInstance(this).getDrawable());

        this.container = (CoordinatorLayout) findViewById(R.id.activity_create_widget_clock_day_center_container);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_text, nameList);
        adapter.setDropDownViewResource(R.layout.spinner_text);
        Spinner locationSpinner = (Spinner) findViewById(R.id.activity_create_widget_clock_day_center_spinner);
        locationSpinner.setAdapter(adapter);
        locationSpinner.setOnItemSelectedListener(this);

        this.showCardSwitch = (Switch) findViewById(R.id.activity_create_widget_clock_day_center_showCardSwitch);
        showCardSwitch.setOnCheckedChangeListener(new ShowCardSwitchCheckListener());

        this.hideRefreshTimeSwitch = (Switch) findViewById(R.id.activity_create_widget_clock_day_center_hideRefreshTimeSwitch);
        hideRefreshTimeSwitch.setOnCheckedChangeListener(new HideRefreshTimeSwitchCheckListener());

        this.blackTextSwitch = (Switch) findViewById(R.id.activity_create_widget_clock_day_center_blackTextSwitch);
        blackTextSwitch.setOnCheckedChangeListener(new BlackTextSwitchCheckListener());

        Button doneButton = (Button) findViewById(R.id.activity_create_widget_clock_day_center_doneButton);
        doneButton.setOnClickListener(this);
    }

    @SuppressLint("SetTextI18n")
    private void refreshWidgetView() {
        if (location.weather == null) {
            return;
        }

        Weather weather = location.weather;

        int[] imageId = WeatherHelper.getWeatherIcon(
                weather.live.weather,
                TimeUtils.getInstance(this).getDayTime(this, location.weather, false).isDayTime());
        widgetIcon.setImageResource(imageId[3]);

        String[] texts = WidgetUtils.buildWidgetDayStyleText(weather);
        widgetWeather.setText(texts[0]);
        widgetTemp.setText(texts[1]);
        widgetRefreshTime.setText(weather.base.location + "." + weather.base.refreshTime);
    }

    /** <br> data. */

    private void initData() {
        this.nameList = new ArrayList<>();
        List<Location> locationList = DatabaseHelper.getInstance(this).readLocationList();
        for (Location l : locationList) {
            nameList.add(l.name);
        }
        this.location = new Location(nameList.get(0), null);

        this.weatherHelper = new WeatherHelper();
        this.locationHelper = new LocationHelper(this);
    }

    /** <br> interface. */

    // on click listener.

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.activity_create_widget_clock_day_center_doneButton:
                SharedPreferences.Editor editor = getSharedPreferences(
                        getString(R.string.sp_widget_clock_day_center_setting),
                        MODE_PRIVATE)
                        .edit();
                editor.putString(getString(R.string.key_location), location.name);
                editor.putBoolean(getString(R.string.key_show_card), showCardSwitch.isChecked());
                editor.putBoolean(getString(R.string.key_hide_refresh_time), hideRefreshTimeSwitch.isChecked());
                editor.putBoolean(getString(R.string.key_black_text), blackTextSwitch.isChecked());
                editor.apply();

                Intent intent = getIntent();
                Bundle extras = intent.getExtras();
                int appWidgetId = 0;
                if (extras != null) {
                    appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID);
                }

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);

                Intent service = new Intent(this, WidgetClockDayCenterAlarmService.class);
                startService(service);
                finish();
                break;
        }
    }

    // on select changed listener(spinner).

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        location = new Location(parent.getItemAtPosition(position).toString(), null);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        location = new Location(parent.getItemAtPosition(0).toString(), null);
    }

    // on check changed listener(switch).

    private class ShowCardSwitchCheckListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                widgetCard.setVisibility(View.VISIBLE);
                widgetClock.setTextColor(ContextCompat.getColor(CreateWidgetClockDayCenterActivity.this, R.color.colorTextDark));
                widgetWeather.setTextColor(ContextCompat.getColor(CreateWidgetClockDayCenterActivity.this, R.color.colorTextDark));
                widgetTemp.setTextColor(ContextCompat.getColor(CreateWidgetClockDayCenterActivity.this, R.color.colorTextDark));
            } else {
                widgetCard.setVisibility(View.GONE);
                if (!blackTextSwitch.isChecked()) {
                    widgetClock.setTextColor(ContextCompat.getColor(CreateWidgetClockDayCenterActivity.this, R.color.colorTextLight));
                    widgetWeather.setTextColor(ContextCompat.getColor(CreateWidgetClockDayCenterActivity.this, R.color.colorTextLight));
                    widgetTemp.setTextColor(ContextCompat.getColor(CreateWidgetClockDayCenterActivity.this, R.color.colorTextLight));
                }
            }
        }
    }

    private class HideRefreshTimeSwitchCheckListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            widgetRefreshTime.setVisibility(hideRefreshTimeSwitch.isChecked() ? View.GONE : View.VISIBLE);
        }
    }

    private class BlackTextSwitchCheckListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                widgetClock.setTextColor(ContextCompat.getColor(CreateWidgetClockDayCenterActivity.this, R.color.colorTextDark));
                widgetWeather.setTextColor(ContextCompat.getColor(CreateWidgetClockDayCenterActivity.this, R.color.colorTextDark));
                widgetTemp.setTextColor(ContextCompat.getColor(CreateWidgetClockDayCenterActivity.this, R.color.colorTextDark));
            } else {
                if (!showCardSwitch.isChecked()) {
                    widgetClock.setTextColor(ContextCompat.getColor(CreateWidgetClockDayCenterActivity.this, R.color.colorTextLight));
                    widgetWeather.setTextColor(ContextCompat.getColor(CreateWidgetClockDayCenterActivity.this, R.color.colorTextLight));
                    widgetTemp.setTextColor(ContextCompat.getColor(CreateWidgetClockDayCenterActivity.this, R.color.colorTextLight));
                }
            }
        }
    }

    // on request name listener.

    @Override
    public void requestLocationSuccess(String locationName) {
        location.realName = locationName;
        weatherHelper.requestWeather(this, location, this);
        DatabaseHelper.getInstance(this).insertLocation(location);
    }

    @Override
    public void requestLocationFailed() {
        SnackbarUtils.showSnackbar(getString(R.string.feedback__location_failed));
    }

    // on request weather listener.

    @Override
    public void requestWeatherSuccess(Weather weather, String locationName) {
        location.weather = weather;
        refreshWidgetView();
        DatabaseHelper.getInstance(this).insertWeather(weather);
        DatabaseHelper.getInstance(this).insertHistory(weather);
    }

    @Override
    public void requestWeatherFailed(String location) {
        this.location.weather = DatabaseHelper.getInstance(this).searchWeather(this.location.realName);
        refreshWidgetView();
        SnackbarUtils.showSnackbar(getString(R.string.feedback_get_weather_failed));
    }
}