package com.cstpierre.telecommande

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import java.io.IOException
import java.util.*
import kotlin.math.min

class FilVideo(
    private val btd: BluetoothDevice?,
    private var view: ImageView,
    private val activity: Activity
) : Thread("Video") {

    var etat: Boolean = false
    lateinit var socket: BluetoothSocket

    private fun byteToInt(bytes: ByteArray): Int {

        return (bytes[0].toInt() and 0xff shl 24) or (bytes[1].toInt() and 0xff shl 16) or (bytes[2].toInt() and 0xff shl 8) or (bytes[3].toInt() and 0xff)
    }

    private fun receptionDonnee(): ByteArray {

        val inputStream = socket.inputStream

        val buffer = ByteArray(4)
        inputStream.read(buffer, 0, 4)
        val streamSize = byteToInt(buffer)

        var positionDansImage = 0
        val image = ByteArray(streamSize)
        while (positionDansImage < streamSize) {
            val quantiteALire = min(8192, streamSize - positionDansImage)
            val quantiteLu = inputStream.read(image, positionDansImage, quantiteALire)
            positionDansImage += quantiteLu
        }

        return image;
    }

    //Envoie un message indicant que l'image est reçue.
    //Ceci permet d'obtimiser le nombre d'images par secondes.
    private fun confirmation() {
        socket.outputStream.write(1)
    }

    private fun afficherImage(bmp: Bitmap) {
        activity.runOnUiThread {
            // some code that needs to be ran in UI thread
            view.setImageBitmap(bmp)
        }
    }

    override fun run() {
        etat = true

        val uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ef")
        socket = btd!!.createInsecureRfcommSocketToServiceRecord(uuid)
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

        try {
            socket.connect()
        } catch (ex: IOException) {
            //TODO: Mettre un message d'erreur
            return
        }

        while (true) {
            if (!etat) {
                socket.close()
                break
            }

            try {
                val imageJpg = receptionDonnee()
                confirmation()
                val image = BitmapFactory.decodeByteArray(imageJpg, 0, imageJpg.size)
                afficherImage(image)
            } catch (ex: Exception) {
                sleep(1_000)
            }
        }
    }
}