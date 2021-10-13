package com.cstpierre.telecommande

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {
    private val mBluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var os: OutputStream
    private lateinit var socket: BluetoothSocket
    private lateinit var videoSocket: BluetoothSocket
    private var minuterie: Long = 0
    private var btd: BluetoothDevice? = null
    private var filVideo: FilVideo? = null

    enum class Commande {
        DROITE, GAUCHE, CENTRE, AVANCE, RECULE, ARRET, ETEINDRE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.droite).setOnTouchListener(boutonDroiteListener)
        findViewById<Button>(R.id.gauche).setOnTouchListener(boutonGaucheListener)
        findViewById<Button>(R.id.avance).setOnTouchListener(boutonAvanceListener)
        findViewById<Button>(R.id.recule).setOnTouchListener(boutonReculeListener)
        findViewById<ImageButton>(R.id.eteindre).setOnTouchListener(boutonEteindreListener)

        var spinner = findViewById<Spinner>(R.id.connections)
        val video = findViewById<ImageView>(R.id.video)

        val bondedDevices = mBluetoothAdapter.bondedDevices

        val choices = bondedDevices.map { btd -> btd.name  }
        val adapter: ArrayAdapter<CharSequence> = ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, choices)
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Set the adapter to th spinner
        spinner.setAdapter(adapter);

        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                btd = bondedDevices.find { btd -> btd.name == spinner.selectedItem }

                socket = btd!!.createInsecureRfcommSocketToServiceRecord(UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"))
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

                    try {
                        socket.connect()
                    } catch (ex: IOException) {
                        Thread.sleep(1_000)
                    }

                os = socket.outputStream

                videoSocket = btd!!.createInsecureRfcommSocketToServiceRecord( UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ef"))
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

                    try {
                        videoSocket.connect()
                    } catch (ex: IOException) {
                        Thread.sleep(1_000)
                    }

                filVideo = FilVideo(videoSocket, video, this@MainActivity)
                filVideo?.start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        os?.write(Commande.CENTRE.ordinal)
        os?.write(Commande.ARRET.ordinal)
        os?.flush()
        os?.close()
        socket?.close()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    override fun onResume() {
        super.onResume()
        if( filVideo!=null) {
            filVideo?.etat = true;
            filVideo?.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if( filVideo!=null)
            filVideo?.etat = false;
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private val boutonAvanceListener = View.OnTouchListener { _, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> os?.write(Commande.AVANCE.ordinal)
            MotionEvent.ACTION_UP -> os?.write(Commande.ARRET.ordinal)
            else -> {}
        }
        false
    }

    private val boutonReculeListener = View.OnTouchListener { _, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> os?.write(Commande.RECULE.ordinal)
            MotionEvent.ACTION_UP -> os?.write(Commande.ARRET.ordinal)
            else -> {}
        }
        false
    }

    private val boutonDroiteListener = View.OnTouchListener { _, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> os?.write(Commande.DROITE.ordinal)
            MotionEvent.ACTION_UP -> os?.write(Commande.CENTRE.ordinal)
            else -> {}
        }
        false
    }

    private val boutonGaucheListener = View.OnTouchListener { _, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> os?.write(Commande.GAUCHE.ordinal)
            MotionEvent.ACTION_UP -> os?.write(Commande.CENTRE.ordinal)
            else -> {}
        }
        false
    }

    private val boutonEteindreListener = View.OnTouchListener { _, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> minuterie = System.nanoTime()
            MotionEvent.ACTION_UP -> if( System.nanoTime() - minuterie > 3_000_000_000) os?.write(
                Commande.ETEINDRE.ordinal
            )
            else -> {}
        }
        false
    }
}