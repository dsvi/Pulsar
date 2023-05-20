package ds.pulsar

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.ds.pulsar.DevInfo
import com.ds.pulsar.Failure
import com.ds.pulsar.searchForBleDevices
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith



/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    @Test
    fun useAppContext() {
        val instr = InstrumentationRegistry.getInstrumentation()
        // Context of the app under test.
        val appContext = instr.targetContext
//        val app = Instrumentation.newApplication(Pulsar::class.java, appContext)
//        instr.callApplicationOnCreate(app)

//        runBlocking {
//            searchForBleDevices().collect() {
//                if (it is DevInfo)
//                    println("testo ${it.name} ${it.mac_address}")
//                if (it is Failure)
//                    println("testo ${it.why}")
//            }
//        }
        assertEquals("ds.pulsar", appContext.packageName)
    }
}