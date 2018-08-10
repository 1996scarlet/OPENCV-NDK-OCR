package ac.ict.humanmotion.ocr

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CROP_REQUEST_CODE -> setUriToView(outUri)
            CAMERA_REQUEST_CODE -> clipPhoto()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getStorageAccessPermissions()
    }

    private fun setUriToView(uri: Uri = outUri) {
        println("using uri")
        diagram = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        imageView.setImageBitmap(diagram)
        OCRthread()
    }

    fun cameraCapture(view: View) = startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, imageUri), CAMERA_REQUEST_CODE)


    private fun clipPhoto(uri: Uri = imageUri) {
        startActivityForResult(Intent("com.android.camera.action.CROP")
                .setDataAndType(uri, "image/*")
                .putExtra("crop", "true")
                .putExtra(MediaStore.EXTRA_OUTPUT, outUri)
                .putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
                .putExtra("return-data", false), CROP_REQUEST_CODE)
    }

    private fun setPicToView(picdata: Intent) {
        println("using intent")
        picdata.extras?.getParcelable<Bitmap>("data")?.let {
            diagram = it
            imageView.setImageBitmap(diagram)

            OCRthread()
        }
    }

    private fun OCRthread() {


        thread {

            val tessBaseAPI = TessBaseAPI()
            tessBaseAPI.init("/storage/emulated/0/", "chi_sim")

            val resultList = mutableListOf("OCR Result:\n")

            srcMat = convertBitMap2Mat(diagram)
            val temp = Mat()

            cvtColor(srcMat, temp, COLOR_RGBA2GRAY)
            Sobel(temp, temp, CV_8U, 1, 0)
            Sobel(temp, temp, CV_8U, 0, 1)
            threshold(temp, temp, 0.0, 255.0, THRESH_OTSU + THRESH_BINARY)

            val element1 = getStructuringElement(MORPH_RECT, Size(30.0, 2.0))
            val element2 = getStructuringElement(MORPH_RECT, Size(24.0, 3.0))

            dilate(temp, temp, element2)

            erode(temp, temp, element1)

            for (i in 0..8) dilate(temp, temp, element2)

            val rects: MutableList<RotatedRect> = mutableListOf()
            val contours: MutableList<MatOfPoint> = mutableListOf()

            findContours(temp, contours, Mat(), RETR_CCOMP, CHAIN_APPROX_SIMPLE)

            contours.forEach {
                val area = contourArea(it)

                if (area < 8000) return@forEach

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

                for (j in 0..3) line(srcMat, pt[j], pt[(j + 1) % 4], Scalar(0.0, 255.0, 0.0), 2)
            }

            diagram = converMat2Bitmat(srcMat)

            runOnUiThread {
                imageView.setImageBitmap(diagram)
            }

            saveResult("/storage/emulated/0/tessdata/result.jpg")

            cvtColor(srcMat, temp, COLOR_RGBA2GRAY)

            rects.asReversed().forEachIndexed { index, it ->
                tessBaseAPI.setImage(converMat2Bitmat(cropMatByRoi(temp, it)))

                var res = ""

                for (t in tessBaseAPI.utF8Text) {
                    if (!rex.contains(t)) res += t
                }

                resultList.add(res)
                println(index.toString() + "-" + res)
            }

//            println(resultList)
            runOnUiThread {
                textView.text = ""
                resultList.forEach {
                    textView.append(it)
                }
            }

            //            val executor = Executors.newFixedThreadPool(rects.size)


//            rects.forEach {it: RotatedRect ->
//                //                val worker = Runnable {
////                    tessBaseAPI.setImage(converMat2Bitmat(cropMatByRoi(srcMat, it)))
////                    println(tessBaseAPI.utF8Text)
////                }
////                executor.execute(worker)
//
//                tessBaseAPI.setImage(converMat2Bitmat(cropMatByRoi(srcMat, it)))
//                println("${tessBaseAPI.utF8Text}")
//            }

            tessBaseAPI.end()
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
        const val CAMERA_REQUEST_CODE = 10086
        const val CROP_REQUEST_CODE = 10085
        const val RES_REQUEST_CODE = 10000
        const val rex = "/*-+)(<>'\\~!@$%&^ -:;[]{}「『…【】_《》oo′\"`\'“”‘’,."

        val imageUri = Uri.parse("file:///storage/emulated/0/tessdata/temp.jpg")
        val outUri = Uri.parse("file:///storage/emulated/0/tessdata/output.jpg")
    }

    lateinit var diagram: Bitmap
    lateinit var srcMat: Mat
//    lateinit var tessBaseAPI: TessBaseAPI

    @TargetApi(23)
    private fun getStorageAccessPermissions() {
        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), RES_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        println("$PERMISSION_TAG:onRequestPermissionsResult: ${grantResults[0]}")
        when (requestCode) {
            RES_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) println("$PERMISSION_TAG:permission get!") else {
                println("$PERMISSION_TAG:permission denied! ")
                finish()
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
