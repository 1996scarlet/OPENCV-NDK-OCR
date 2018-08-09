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
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.CvType.CV_8U
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.*
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    lateinit var diagram: Bitmap

    lateinit var srcMat: Mat

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
                        }

                        else -> {
                        }
                    }
                }
            }
        }

        thread {

            srcMat = convertBitMap2Mat(BitmapFactory.decodeFile("/storage/emulated/0/tessdata/camera.jpg"))
            val temp = Mat()

            cvtColor(srcMat, temp, COLOR_RGBA2GRAY)
            Sobel(temp, temp, CV_8U, 1, 0)
            threshold(temp, temp, 0.0, 255.0, THRESH_OTSU + THRESH_BINARY)

            val element1 = getStructuringElement(MORPH_RECT, Size(30.0, 9.0))
            val element2 = getStructuringElement(MORPH_RECT, Size(24.0, 3.0))

            dilate(temp, temp, element2)
            dilate(temp, temp, element2)

//            erode(temp, temp, element1)

            dilate(temp, temp, element2)
            dilate(temp, temp, element2)


            val rects: MutableList<RotatedRect> = mutableListOf()
            val contours: MutableList<MatOfPoint> = mutableListOf()

            findContours(temp, contours, Mat(), RETR_CCOMP, CHAIN_APPROX_SIMPLE)

            contours.forEach {
                val area = contourArea(it)

                if (area < 1000) return@forEach

                val newPoint = MatOfPoint2f()
                it.convertTo(newPoint, CvType.CV_32F)

                approxPolyDP(newPoint, MatOfPoint2f(), 0.001 * arcLength(newPoint, true), true)

                val rect = minAreaRect(newPoint)

//                val m_width = rect.boundingRect().width
//                val m_height = rect.boundingRect().height
//
//                if (m_height > m_width * 1.2) return@forEach

                rects.add(rect)
            }



            rects.forEach {

                val pt = arrayOf(Point(), Point(), Point(), Point())
                it.points(pt)

                for (j in 0..3) {
                    line(srcMat, pt[j], pt[(j + 1) % 4], Scalar(0.0, 255.0, 0.0), 2)
                }

            }

            diagram = converMat2Bitmat(srcMat)

            saveResult("/storage/emulated/0/tessdata/result.jpg")

            val msg = Message.obtain()
            msg.obj = BITMAP_TAG
            handle.sendMessage(msg)

            rects.forEach {
                val src = converMat2Bitmat(cropMatByRoi(srcMat, it))
                tessBaseAPI.setImage(src)
                println(tessBaseAPI.utF8Text)
            }
        }
    }

    fun converMat2Bitmat(img: Mat): Bitmap {
        val width = img.width()
        val hight = img.height()


        val bmp: Bitmap
        bmp = Bitmap.createBitmap(width, hight, Bitmap.Config.ARGB_8888)
        val tmp: Mat
        tmp = if (img.channels() == 1) Mat(width, hight, CvType.CV_8UC1, Scalar(1.0)) else Mat(width, hight, CvType.CV_8UC3, Scalar(3.0))
        try {
            if (img.channels() == 3)
                cvtColor(img, tmp, Imgproc.COLOR_RGB2BGRA)
            else if (img.channels() == 1)
                cvtColor(img, tmp, Imgproc.COLOR_GRAY2RGBA)
            Utils.matToBitmap(tmp, bmp)
        } catch (e: CvException) {
            Log.d("Expection", e.message)
        }

        return bmp
    }

    fun convertBitMap2Mat(rgbaImage: Bitmap): Mat {
        val rgbaMat = Mat(rgbaImage.height, rgbaImage.width, CvType.CV_8UC4)
        val bmp32 = rgbaImage.copy(Bitmap.Config.ARGB_8888, true)
        Utils.bitmapToMat(bmp32, rgbaMat)

        val rgbMat = Mat(rgbaImage.height, rgbaImage.width, CvType.CV_8UC3)
        cvtColor(rgbaMat, rgbMat, Imgproc.COLOR_RGBA2BGR, 3)
        return rgbMat
    }

    private fun saveResult(path: String) {
        val ff = File(path)
        ff.createNewFile()
        val ostream = FileOutputStream(ff)
        diagram.compress(Bitmap.CompressFormat.JPEG, 100, ostream)
        ostream.close()
    }

    external fun selfEdge(bitmap: Any)

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


    fun cropMatByRoi(cpsrcMat: Mat, rotatedRect: RotatedRect): Mat {

        var rotated: Mat
        var angle: Double
        if (rotatedRect.size.width < rotatedRect.size.height) {
            angle = rotatedRect.angle + 180
        } else {
            angle = rotatedRect.angle + 90
        }
        angle -= 90
        var rad: Double
        val maxrectbian = if (rotatedRect.size.height > rotatedRect.size.width) rotatedRect.size.height else rotatedRect.size.width
        val minrectbian = if (rotatedRect.size.height < rotatedRect.size.width) rotatedRect.size.height else rotatedRect.size.width
        val radw = cpsrcMat.width() / maxrectbian
        val radh = cpsrcMat.height() / minrectbian
        rad = if (radw < radh) radw else radh
        rad *= 0.95
        rotated = rotateImage1(cpsrcMat, angle)

        val pading = 5
        val angleHUD = angle * Math.PI / 180.0 // 弧度
        val sin = Math.abs(Math.sin(angleHUD))
        val cos = Math.abs(Math.cos(angleHUD))
        val tan = Math.abs(Math.tan(angleHUD))
        val oldx = rotatedRect.center.x
        val oldy = rotatedRect.center.y
        var newpX = 0.0
        var newpY = 0.0
        if (angle < 0) {
            newpX = cpsrcMat.height() * sin + oldx * cos - oldy * sin//新坐标系下rect中心坐标
            newpY = oldy / cos + (oldx - oldy * tan) * sin//新坐标系下rect中心坐标
        } else if (angle >= 0) {
            newpX = oldx * cos + oldy * sin
            newpY = oldy / cos + (cpsrcMat.width() - (oldx + oldy * tan)) * sin
        }

        var startrow = (newpY - minrectbian / 2).toInt() - pading
        if (startrow < 0) startrow = 0

        var endrow = (newpY + minrectbian / 2).toInt() + pading
        if (endrow >= rotated.height()) endrow = rotated.height()

        var startcls = (newpX - maxrectbian / 2).toInt() - pading
        if (startcls < 0) startcls = 0

        var endcls = (newpX + maxrectbian / 2).toInt() + pading
        if (endcls >= rotated.width()) endcls = rotated.width()
        rotated = rotated.submat(startrow, endrow, startcls, endcls)
        return rotated
    }

    fun rotateImage1(img: Mat, degree: Double): Mat {
        val angle = degree * Math.PI / 180.0 // 弧度
        val a = Math.sin(angle)
        val b = Math.cos(angle)
        val width = img.width()
        val height = img.height()
        val width_rotate = (height * Math.abs(a) + width * Math.abs(b))
        val height_rotate = (width * Math.abs(a) + height * Math.abs(b))
        var map_matrix = Mat(2, 3, CvType.CV_32F)
        val center = Point(width / 2.0, height / 2.0)
        map_matrix = Imgproc.getRotationMatrix2D(center, degree, 1.0)
        map_matrix.put(0, 2, map_matrix.get(0, 2)[0] + (width_rotate - width) / 2)
        map_matrix.put(1, 2, map_matrix.get(1, 2)[0] + (height_rotate - height) / 2)
        val rotated = Mat()
        Imgproc.warpAffine(img, rotated, map_matrix, Size(width_rotate, height_rotate), Imgproc.INTER_LINEAR or Imgproc.WARP_FILL_OUTLIERS, 0, Scalar(255.0, 255.0, 255.0))
        return rotated
    }
}
