package com.egco428.fon

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.estimote.coresdk.common.requirements.SystemRequirementsChecker
import com.estimote.coresdk.observation.region.beacon.BeaconRegion
import com.estimote.coresdk.recognition.packets.Beacon
import com.estimote.coresdk.service.BeaconManager
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

import android.util.Log


class MainActivity : AppCompatActivity() {
    private var beaconManager: BeaconManager? = null
    private var region: BeaconRegion? = null
    private var PLACES_BY_BEACONS: Map<String, List<String>>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val placesByBeacons = HashMap<String, ArrayList<String>>()
        placesByBeacons.put("38845:58352", arrayListOf("Blue", "Mint", "Coco", "Mash"))
        placesByBeacons.put("51284:49927", arrayListOf("Blue", "Mint", "Coco", "Mash"))
        placesByBeacons.put("31937:52697", arrayListOf("Blue", "Mint", "Coco", "Mash"))
        placesByBeacons.put("12436:27016", arrayListOf("Blue", "Mint", "Coco", "Mash"))
        PLACES_BY_BEACONS = Collections.unmodifiableMap(placesByBeacons)
        beaconManager = BeaconManager(this)
        region = BeaconRegion("ranged region",
            UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), 51284, 49927)
        beaconManager!!.connect {
            beaconManager!!.startMonitoring(BeaconRegion("monitored region",
                UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), 51284, 49927))
        }

        beaconManager!!.setRangingListener(object :BeaconManager.BeaconRangingListener{
            override fun onBeaconsDiscovered(
                beaconRegion: BeaconRegion?,
                beacons: MutableList<Beacon>?
            ) {
                if (!beacons!!.isEmpty()) {
                    val nearestBeacon = beacons.get(0)
                    val places = placesNearBeacon(nearestBeacon)
                    // TODO: update the UI here
                    Log.d("Airport", "Nearest places: $places")
                }
            }

        })
        beaconManager!!.setMonitoringListener(object : BeaconManager.BeaconMonitoringListener {
            override fun onExitedRegion(beaconRegion: BeaconRegion?) {

            }

            override fun onEnteredRegion(beaconRegion: BeaconRegion?, beacons: MutableList<Beacon>?) {
                showNotification(
                    "Your gate closes in 47 minutes.",
                    "Current security wait time is 15 minutes, "
                            + "and it's a 5 minute walk from security to the gate. "
                            + "Looks like you've got plenty of time!");
                placesNearBeacon(beacons!![0])
            }

        })
    }
    private fun showNotification(title: String, message: String) {
        val notifyIntent = Intent(this, MainActivity::class.java)
        notifyIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivities(this, 0,
            arrayOf(notifyIntent), PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notification.defaults = notification.defaults or Notification.DEFAULT_SOUND
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }
    private fun placesNearBeacon(beacon: Beacon): List<String>? {
        var beaconKey = String.format("%d:%d", beacon.major, beacon.minor)
        if (PLACES_BY_BEACONS!!.containsKey(beaconKey)) {
            Toast.makeText(this,"fon $beaconKey", Toast.LENGTH_LONG).show()
            return PLACES_BY_BEACONS?.get(beaconKey)
        }
        return Collections.emptyList()
    }

    override fun onResume() {
        super.onResume()
        SystemRequirementsChecker.checkWithDefaultDialogs(this)
        beaconManager!!.connect(object : BeaconManager.ServiceReadyCallback {
            override fun onServiceReady() {
                beaconManager!!.startRanging(region);
            }

        })
    }

    override fun onPause() {
        beaconManager!!.stopRanging(region);
        super.onPause()
    }
}
