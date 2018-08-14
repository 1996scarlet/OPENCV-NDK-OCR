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
            //            mRgba = it
            selfBinary(it.nativeObjAddr, mRgba.nativeObjAddr)
        }

//        Imgproc.cvtColor(inputFrame?.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4)
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
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            println("FAIL")
        } else {
            println("SUCCESS")
            cameraView.enableView()
        }
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

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}
