package ac.ict.humanmotion.ocr

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    lateinit var diagram: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getStorageAccessPermissions()

        val tessBaseAPI = TessBaseAPI()
        tessBaseAPI.init("/storage/emulated/0/", "chi_sim")

        val handle = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message?) {
                msg.let {
                    when (it?.obj) {
                        BITMAP_TAG -> {
                            imageView.setImageBitmap(diagram)
                            thread {
                                tessBaseAPI.setImage(diagram)
                                println(tessBaseAPI.utF8Text)
                                tessBaseAPI.end()
                            }
                        }

                        else -> {
                        }
                    }
                }
            }
        }

        thread {
            diagram = Bitmap.createScaledBitmap(BitmapFactory.decodeFile("/storage/emulated/0/tessdata/camera.jpg"), 1000, 1000, true)
            //getEdge(diagram)
            selfBinary(diagram)
            selfDilate(diagram)
            selfErode(diagram, 30, 5)
            selfDilate(diagram)
            selfRect(diagram)

            saveResult("/storage/emulated/0/tessdata/result.jpg")

            val msg = Message.obtain()
            msg.obj = BITMAP_TAG
            handle.sendMessage(msg)
        }
    }

    private fun saveResult(path: String) {
        val ff = File(path)
        ff.createNewFile()
        val ostream = FileOutputStream(ff)
        diagram.compress(Bitmap.CompressFormat.JPEG, 100, ostream)
        ostream.close()
    }

    external fun selfBinary(bitmap: Any)

    external fun selfDilate(bitmap: Any, p1: Int = 24, p2: Int = 3)
    external fun selfErode(bitmap: Any, p1: Int = 30, p2: Int = 9)

    external fun selfRect(bitmap: Any)


    companion object {

        init {
            System.loadLibrary("native-lib")
        }

        const val PERMISSION_TAG = "RequestPermissions"
        const val BITMAP_TAG = "DONE"
    }

    @TargetApi(23)
    private fun getStorageAccessPermissions() {
        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 10086)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        println("$PERMISSION_TAG:onRequestPermissionsResult: ${grantResults[0]}")
        when (requestCode) {
            10086 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) println("$PERMISSION_TAG:permission get!") else println("$PERMISSION_TAG:permission denied! ")
            else -> {
            }
        }
    }
}
