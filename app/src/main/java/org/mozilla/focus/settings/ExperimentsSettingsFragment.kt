/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v14.preference.SwitchPreference
import android.support.v7.preference.PreferenceFragmentCompat
import com.jakewharton.processphoenix.ProcessPhoenix
import mozilla.components.service.fretboard.ExperimentDescriptor
import mozilla.components.service.fretboard.Fretboard
import org.mozilla.focus.FocusApplication
import org.mozilla.focus.R
import org.mozilla.focus.web.Config
import org.mozilla.focus.web.ENGINE_PREF_STRING_KEY

const val EXPERIMENTS_JSON_FILENAME = "experiments.json"
const val EXPERIMENTS_BASE_URL = "https://settings.prod.mozaws.net/v1"
const val EXPERIMENTS_BUCKET_NAME = "main"
const val EXPERIMENTS_COLLECTION_NAME = "focus-experiments"

val experimentDescriptor = ExperimentDescriptor(Config.EXPERIMENT_DESCRIPTOR_GECKOVIEW_ENGINE)

class ExperimentsSettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val FRAGMENT_TAG = "ExperimentSettings"
    }

    private var rendererPreferenceChanged = false
    private lateinit var fretboard: Fretboard
    private lateinit var enginePreference: SwitchPreference
    lateinit var application: FocusApplication

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.experiments_settings)
        enginePreference = preferenceManager
                .findPreference(ENGINE_PREF_STRING_KEY) as SwitchPreference
        application = activity?.application as FocusApplication
        fretboard = application.fretboard
        enginePreference.isChecked = fretboard.isInExperiment(application, experimentDescriptor)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen?.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen?.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        if (rendererPreferenceChanged) {
            fretboard.setOverride(application, experimentDescriptor, enginePreference.isChecked)
            val launcherIntent = activity?.packageManager?.getLaunchIntentForPackage(activity?.packageName)
            ProcessPhoenix.triggerRebirth(context, launcherIntent)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            ENGINE_PREF_STRING_KEY -> {
                rendererPreferenceChanged = true
            }
        }
    }
}
