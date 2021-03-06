/*
 * Copyright (C) 2014 Priyesh Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.priyesh.hexatime;

import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Toast;

public class HexatimeService extends WallpaperService{

	private static final String TAG = "Wallpaper";
	public static final String SHARED_PREFS_NAME="hexatime_settings";
	public int oneSecond = 1000;
	public int hour, min, sec, twelveHour;
	public Calendar cal;
	private SharedPreferences mPrefs = null;
	
	private int fontStyleValue = 1; // 0 is regular, 1 is light
	private Typeface fontStyle;
	
	private int clockSizeValue = 1; // 0 = small, 1 = medium, 2 = large
	private int clockSize;
	
	private int clockAlignmentValue = 1; // 0 = top, 1 = center, 2 = bottom
	private float clockAlignment;
	
	private int clockStyleValue = 1;
	private String clockStyle;
	
	private boolean clockHideValue = false;

	@Override
	public Engine onCreateEngine() {
		return new HexatimeEngine(this);
	}

	private class HexatimeEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {

		private boolean mVisible = false;
		private final Handler mHandler = new Handler();

		private Canvas c;
		private Paint hexClock, bg;

		private final Runnable mUpdateDisplay = new Runnable() {

			@Override
			public void run() {
				draw();
			}};

			HexatimeEngine(WallpaperService ws) {

				mPrefs = HexatimeService.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
				mPrefs.registerOnSharedPreferenceChangeListener(this);
				onSharedPreferenceChanged(mPrefs, null);
			}

			private void draw() {
				cal = Calendar.getInstance();
				hour = cal.get(Calendar.HOUR_OF_DAY);
				min = cal.get(Calendar.MINUTE);
				sec = cal.get(Calendar.SECOND);
				twelveHour = cal.get(Calendar.HOUR);

				SurfaceHolder holder = getSurfaceHolder();
				c = null;
				try {
					c = holder.lockCanvas();
					if (c != null) {
						hexClock = new Paint();
						bg = new Paint();
						
						hexClock.setTextSize(clockSize);
						hexClock.setTypeface(fontStyle);
						hexClock.setAntiAlias(true);
						
						String hexTime = String.format(clockStyle, hour, min, sec ); // 24 hour hex triplet time
						
						float d = hexClock.measureText(hexTime, 0, hexTime.length());
						int offset = (int) d / 2;
						int w = c.getWidth();
						int h = c.getHeight();

						bg.setColor(Color.argb(255, hour, min, sec));
						c.drawRect(0, 0, w, h, bg);

						if(!clockHideValue){
							hexClock.setColor(Color.WHITE);
						}
						else {
							hexClock.setColor(Color.TRANSPARENT);
						}
						c.drawText(hexTime, w/2- offset, clockAlignment, hexClock);

					}
				} finally {
					if (c != null)
						holder.unlockCanvasAndPost(c);
				}
				mHandler.removeCallbacks(mUpdateDisplay);
				if (mVisible) {
					mHandler.postDelayed(mUpdateDisplay, oneSecond);
				}
			}

			@Override
			public void onVisibilityChanged(boolean visible) {
				mVisible = visible;
				if (visible) {
					draw();
				} else {
					mHandler.removeCallbacks(mUpdateDisplay);
				}
			}

			@Override
			public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				draw();
			}

			@Override
			public void onSurfaceDestroyed(SurfaceHolder holder) {
				super.onSurfaceDestroyed(holder);
				mVisible = false;
				mHandler.removeCallbacks(mUpdateDisplay);
			}

			@Override
			public void onDestroy() {
				super.onDestroy();
				mVisible = false;
				mHandler.removeCallbacks(mUpdateDisplay);
			}

			@Override
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				if(key != null){
					if(key.equals("FONT_STYLE")){
						changeFontStyle(prefs.getString("FONT_STYLE", "1"));
					}
					else if(key.equals("CLOCK_SIZE")){
						changeClockSize(prefs.getString("CLOCK_SIZE", "1"));
					}
					else if(key.equals("CLOCK_ALIGNMENT")){
						changeClockAlignment(prefs.getString("CLOCK_ALIGNMENT", "1"));
					}
					else if(key.equals("CLOCK_STYLE")){
						changeClockStyle(prefs.getString("CLOCK_STYLE", "1"));
					}
					else if(key.equals("CLOCK_HIDE")){
						changeClockHide(prefs.getBoolean("CLOCK_HIDE", false));
					}
				}
				else {	                        
					changeFontStyle(prefs.getString("FONT_STYLE", "1"));
					changeClockSize(prefs.getString("CLOCK_SIZE", "1"));
					changeClockAlignment(prefs.getString("CLOCK_ALIGNMENT", "1"));
					changeClockStyle(prefs.getString("CLOCK_STYLE", "1"));
					changeClockHide(prefs.getBoolean("CLOCK_HIDE", false));
				}
				return;
			}

			private void changeFontStyle(String value){
				fontStyleValue = Integer.parseInt(value);
				if(fontStyleValue == 0){ // regular
					fontStyle = Typeface.createFromAsset(getAssets(), "Lato.ttf");
				}
				else if (fontStyleValue == 1){ // light
					fontStyle = Typeface.createFromAsset(getAssets(), "LatoLight.ttf");                    
				}
				return;
			}
			
			private void changeClockSize(String value){
				clockSizeValue = Integer.parseInt(value);
				if(clockSizeValue == 0){ // small
					clockSize = (getResources().getDimensionPixelSize(R.dimen.clockFontSizeSmall));
				}
				else if (clockSizeValue == 1){ // medium
					clockSize = (getResources().getDimensionPixelSize(R.dimen.clockFontSizeMed));
				}
				else if (clockSizeValue == 2){ //large
					clockSize = (getResources().getDimensionPixelSize(R.dimen.clockFontSizeLarge));
				}
				return;
			}
			
			private void changeClockAlignment(String value){
				clockAlignmentValue = Integer.parseInt(value);				
				WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
				Display display = wm.getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				int h = size.y;
				
				if(clockAlignmentValue == 0){ // top
					clockAlignment = (float) (h - (h*0.65));
				}
				else if (clockAlignmentValue == 1){ // center
					clockAlignment = (float) (h - (h*0.50));
				}
				else if (clockAlignmentValue == 2){ //bottom
					clockAlignment = (float) (h - (h*0.35));
				}
				return;
			}
			
			private void changeClockStyle(String value){
				clockStyleValue = Integer.parseInt(value);
				if(clockStyleValue == 0){ // Hide #
					clockStyle = "%02d%02d%02d";
				}
				else if (clockStyleValue == 1){ // Show #
					clockStyle = "#%02d%02d%02d";              
				}
				return;
			}
			
			private void changeClockHide(boolean value){
				clockHideValue = value;
			}
	}
}
