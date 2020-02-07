package overview.map

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import kotlinx.android.synthetic.main.activity_license.*

class LicenseActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)

        title = getString(R.string.license_activity_title)
        licenseTextView.movementMethod = LinkMovementMethod.getInstance()
        dataGovPastaTextView.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
