package ac.ict.humanmotion.ocr

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class TrackerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker)

        savedInstanceState ?: supportFragmentManager.beginTransaction()
                .replace(R.id.container, Camera2VideoFragment.newInstance())
                .commit()
    }
}
