package ac.ict.humanmotion.ocr.CVCamera

import ac.ict.humanmotion.ocr.R
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_camera.*
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat


class CameraActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var mRgba: Mat

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        inputFrame?.rgba()?.let {
            selfBinary(it.nativeObjAddr, mRgba.nativeObjAddr)
        }

        return mRgba
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
        println("onCameraViewStarted")
    }

    override fun onCameraViewStopped() {
        mRgba.release()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        cameraView.setCvCameraViewListener(this)

        println(avformatInfo())
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            println("FAIL")
        } else cameraView.enableView()
    }

    override fun onPause() {
        super.onPause()
        if (cameraView != null) {
            cameraView.disableView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (cameraView != null) {
            cameraView.disableView()
        }
    }

    external fun selfBinary(old_addr: Long, new_addr: Long)
    external fun avformatInfo(): String

    companion object {
        init {
            System.loadLibrary("native-lib")
            System.loadLibrary("avcodec")
            System.loadLibrary("avfilter")
            System.loadLibrary("avformat")
            System.loadLibrary("avutil")
            System.loadLibrary("swresample")
            System.loadLibrary("swscale")
        }
    }
}
