/**
 * 
 */
package com.divyasri.kotlinweather.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.divyasri.kotlinweather.Consts.Constants;

import java.util.HashSet;
import java.util.Set;

/**
 * A sample cache class that will store the values for the length of the
 * session.
 * 
 * @author Divyasri
 * @version 1.0
 */
public final class Cache {

	public static void add(Context _ctx, String key, String value) {
		SharedPreferences prefs = _ctx.getSharedPreferences(Constants.SP_WEATHER_KIT,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, value);
		editor.apply();
	}
	public static String get(Context _ctx, String key) {
		SharedPreferences prefs = _ctx.getSharedPreferences(Constants.SP_WEATHER_KIT,
				Context.MODE_PRIVATE);
		return prefs.getString(key, Constants.STR_BLANK);
	}

}
