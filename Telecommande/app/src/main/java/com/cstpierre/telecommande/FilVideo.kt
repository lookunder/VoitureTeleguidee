package com.cstpierre.telecommande

import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.widget.ImageView
import android.app.Activity
import android.graphics.BitmapFactory
import java.lang.Exception
import kotlin.math.min

class FilVideo(private val socket: BluetoothSocket, private var view: ImageView, private val activity : Activity) : Thread("Video") {

    var etat:Boolean = false

    // Il faut un format ARGB ou A est xFF dans notre cas
    private fun byteToInt(bytes: ByteArray, debut:Int, canauxCouleurs:Int): Int {

        var couleur = 0
        if( canauxCouleurs==1 ) {
            couleur = bytes[debut].toInt() and 0xff
        }
        if( canauxCouleurs==3 ) {
            couleur = 0xff and 0xff shl 24 or (bytes[debut+2].toInt() and 0xff shl 16) or (bytes[debut+1].toInt() and 0xff shl 8) or (bytes[debut].toInt() and 0xff)
        }

        return couleur
    }

    private fun byteToInt2(bytes: ByteArray): Int {

        return (bytes[0].toInt() and 0xff shl 24) or (bytes[1].toInt() and 0xff shl 16) or (bytes[2].toInt() and 0xff shl 8) or (bytes[3].toInt() and 0xff)
    }

    private fun receptionDonnee(image:ByteArray) : Int {
        var positionDansImage = 0
        val inputStream = socket.inputStream
        positionDansImage = inputStream.read(image, positionDansImage, 4)
        val streamSize = byteToInt2(image)
        while( positionDansImage<((streamSize+4)-1) ) {
            val quantiteALire = min(8192,(streamSize+4)-positionDansImage)
            val quantiteLu = inputStream.read(image, positionDansImage, quantiteALire)
            positionDansImage += quantiteLu
        }

        return streamSize;
    }

    //Envoie un message indicant que l'image est reçue.
    //Ceci permet d'obtimiser le nombre d'images par secondes.
    private fun confirmation() {
        socket.outputStream.write(1)
    }

    private fun afficherImage(bmp:Bitmap) {
        activity.runOnUiThread {
            // some code that needs to be ran in UI thread
            view.setImageBitmap(bmp)
        }
    }

    override fun run() {
        etat = true

        val image = ByteArray(640*480*3)

        while(true) {
            if(!etat)
                break

            try{
                val streamSize = receptionDonnee(image)
                confirmation()
                val toto = BitmapFactory.decodeByteArray(image,4, streamSize)
                afficherImage(toto)
            }catch (ex: Exception) {
                sleep(1_000)
            }
        }
    }
}