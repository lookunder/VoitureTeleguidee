package com.cstpierre.telecommande

import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.core.graphics.set
import android.app.Activity
import java.lang.Exception

class FilVideo(private val socket: BluetoothSocket, var view: ImageView, val activity : Activity) : Thread("Video") {

    var etat:Boolean = false

    // Il faut un format ARGB ou A est xFF dans notre cas
    private fun byteToInt(bytes: ByteArray, debut:Int, canauxCouleurs:Int): Int {

        var couleur : Int = 0
        if( canauxCouleurs==1 ) {
            couleur = bytes[debut].toInt() and 0xff
        }
        if( canauxCouleurs==3 ) {
            couleur = 0xff and 0xff shl 24 or (bytes[debut+2].toInt() and 0xff shl 16) or (bytes[debut+1].toInt() and 0xff shl 8) or (bytes[debut].toInt() and 0xff)
        }

        return couleur;
    }

    override fun run() {
        etat = true
        var inputStream = socket.inputStream
        val largeur = 64
        val hauteur = 48
        val canaux = 3
        val streamSize = hauteur*largeur*canaux
        val image = ByteArray(streamSize)
        val buffer = ByteArray(streamSize)

        val conf = Bitmap.Config.ARGB_8888
        val bmp = Bitmap.createBitmap(hauteur, largeur, conf)

        while(true) {
            if(etat==false)
                break

            try{
                var positionDansImage = 0
                while( positionDansImage<(streamSize-1) ) {
                    val resRead = inputStream.read(buffer)
                    val espaceRestant = streamSize-positionDansImage
                    buffer.copyInto(image,positionDansImage,0,Math.min(resRead,espaceRestant))
                    positionDansImage += resRead;
                }

                for( i in 0 until hauteur) {
                    for (j in 0 until largeur) {
                        val position = canaux*(i*largeur + j)
                        val couleur = byteToInt(image,position, canaux)
                        bmp.set(i,j,couleur)
                    }
                }

                activity.runOnUiThread(Runnable {
                    // some code that needs to be ran in UI thread
                    view.setImageBitmap(bmp)
                })
            }catch (ex: Exception) {
                Thread.sleep(1_000)
            }
        }
    }
}