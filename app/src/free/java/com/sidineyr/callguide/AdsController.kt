package com.sidineyr.callguide

import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** Free builds have no ads SDK, internet permission, or advertising identifier. */
class AdsController(private val activity: AppCompatActivity) {
    fun setAdult(adult: Boolean) = Unit
    fun canShow() = false
    fun attach(container: LinearLayout) = Unit
    fun pause() = Unit
    fun resume() = Unit
    fun destroyView() = Unit
    fun privacyOptions() {
        Toast.makeText(activity, "Esta versão não contém publicidade.", Toast.LENGTH_LONG).show()
    }
}
