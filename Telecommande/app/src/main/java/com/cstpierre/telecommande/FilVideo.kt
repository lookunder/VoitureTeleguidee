package com.cstpierre.telecommande

import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.core.graphics.set
import android.app.Activity
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

    private fun generateBitmap(hauteur:Int, largeur:Int, canaux:Int, image:ByteArray, bmp:Bitmap)
    {
        for( i in 0 until hauteur) {
            for (j in 0 until largeur) {
                val position = canaux * (i * largeur + j)
                val couleur = byteToInt(image, position, canaux)
                bmp[i, j] = couleur
            }
        }
    }

    private fun receptionDonnee(streamSize:Int, image:ByteArray) {
        var positionDansImage = 0
        val inputStream = socket.inputStream
        while( positionDansImage<(streamSize-1) ) {
            val quantiteALire = min(8192,streamSize-positionDansImage)
            val quantiteLu = inputStream.read(image, positionDansImage, quantiteALire)
            positionDansImage += quantiteLu
        }
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

        val largeur = 64
        val hauteur = 48
        val canaux = 3
        val streamSize = hauteur*largeur*canaux
        val image = ByteArray(streamSize)

        val conf = Bitmap.Config.ARGB_8888
        val bmp = Bitmap.createBitmap(hauteur, largeur, conf)

        while(true) {
            if(!etat)
                break

            try{
                receptionDonnee(streamSize, image)
                confirmation()
                generateBitmap(hauteur, largeur, canaux, image, bmp)
                afficherImage(bmp)
            }catch (ex: Exception) {
                sleep(1_000)
            }
        }
    }
}