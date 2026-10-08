package com.ds.pulsar

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ds.pulsar.ui.theme.PulsarTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.concurrent.TimeoutException
import kotlin.math.max
import kotlin.math.min


private lateinit
var navController_: NavHostController
val navController by ::navController_

private lateinit
var activity : Activity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            PulsarTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    navController_ = rememberNavController()
                    NavHost(navController = navController, startDestination = Screens.main) {
                        val durationMs = 700
                        val animSpec = tween<IntOffset>(durationMs, easing = EaseInOutQuad)
                        composable(
                            Screens.main,
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = animSpec
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = animSpec
                                )
                            }
                        ) { MainScreen() }
                        composable(
                            Screens.devList,
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = animSpec
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = animSpec
                                )
                            }
                        ) { DeviceDiscoverScreen() }
                    }
                }
            }
        }
    }
}

private object Screens{
    const val main = "main"
    const val devList = "device list"
}

@Composable
fun MainScreen() {
    WhenBtIsReady {
        val requiresSearch = remember {
            heartRateStream.requiresSearchForDevice
        }
        if (requiresSearch)
            navController.navigate(Screens.devList)
        else
            MainUI()
    }
}

@Composable
private fun MainUI() {
    BoxWithConstraints {
        val topButtonPadding = 6.dp
        var askAgain by remember{
            mutableStateOf(gotToAskAboutDisablingBt)
        }
        fun quit(disableBt: Boolean ){
            if (disableBt)
                btAdapter.disable()
            activity.finishAndRemoveTask()
        }
        var showExitDialog by remember{ mutableStateOf(false) }
        if (showExitDialog) {
            Dialog(onDismissRequest = {showExitDialog = false}) {
                Card {
                    val mdf = Modifier.padding(8.dp)
                    Text(
                        modifier = mdf,
                        text = "Turn off bluetooth?",
                        style=MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        modifier = mdf,
                        text = """
                            Since I have turned on bluetooth, I can turn it off for ya.
                            But I will only ever do it, when I was the one, who have turned it on.""".trimIndent(),
                    )
                    Row(
                        modifier = mdf,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Checkbox(
                            checked = !askAgain,
                            onCheckedChange = {
                                askAgain = !it
                            }
                        )
                        Text("Don't ask again, always apply current choice")
                    }
                    Row(
                        modifier = mdf.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ){
                        ElevatedButton(
                            onClick = {
                                if (!askAgain) {
                                    disableBt = true
                                    gotToAskAboutDisablingBt = askAgain
                                }
                                quit(true)
                            }
                        ){
                            Text("Turn it off")
                        }
                        ElevatedButton(
                            onClick = {
                                if (!askAgain) {
                                    disableBt = false
                                    gotToAskAboutDisablingBt = askAgain
                                }
                                quit(false)
                            }
                        ){
                            Text("Leave it on")
                        }
                    }
                }
            }
        }
        ElevatedButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(topButtonPadding),
            onClick = {
                if (mayDisableBt){
                    if (gotToAskAboutDisablingBt) {
                        showExitDialog = true
                    }
                    else{
                        quit(disableBt)
                    }
                }
                else
                    quit(false)
            }
        ){
            Icon(
                Icons.Default.Close,
                contentDescription = "Close the app",
            )
        }
        val landscape = maxWidth > maxHeight
        //region timer state
        val pulse = remember{ mutableStateOf("---") }
        val time = remember{ mutableStateOf("---") }
        var battery by remember{ mutableStateOf("") }
        LaunchedEffect(null ) {
            pulse.value = "---"
            heartRateStream.data.collect {
                pulse.value = it.pulse.toString()
                if (it.batteryLevel != null)
                    battery = it.batteryLevel.toString() + "%"
            }
        }
        val currentSeconds = {
            System.currentTimeMillis() / 1000
        }
        var iconNdx by rememberSaveable{ mutableStateOf(0) }
        var startTime by rememberSaveable{ mutableStateOf( currentSeconds() ) }
        var accumulatedTime by rememberSaveable{ mutableStateOf( 0L ) }
        LaunchedEffect(null ) {
            while (true) {
                delay(950)
                if ( iconNdx != 0 )
                    continue
                val td = accumulatedTime +
                        currentSeconds() - startTime
                val h = td / 3600
                val m = (td % 3600) / 60
                val s = td % 60
                if (h == 0L)
                    time.value = "%02d:%02d".format(m ,s)
                else
                    time.value = "%d:%02d:%02d".format(h, m ,s)
            }
        }
        val icons by remember{
            mutableStateOf( arrayOf(Icons.Default.Pause, Icons.Default.PlayArrow ) )
        }
        val reset = {
            accumulatedTime = 0
            startTime = currentSeconds()
            iconNdx = 0
        }
        val togglePause = {
            if (iconNdx == 0)
                accumulatedTime += currentSeconds() - startTime
            else
                startTime = currentSeconds()
            iconNdx = ++iconNdx % icons.size
        }
        // endregion
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(topButtonPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElevatedButton(
                onClick = { navController.navigate(Screens.devList) }
            ){
                Icon(
                    Icons.AutoMirrored.Filled.BluetoothSearching,
                    contentDescription = "List of available BLE devices",
                )
            }
            // in landscape there is no room for it under the digits
            if (landscape)
                BatteryIndicator(battery)
        }
        if (landscape)
            HorizontalMainUI(
                pulse = pulse.value,
                time = time.value,
                pauseIcon = icons[iconNdx],
                onReset = reset,
                onTogglePause = togglePause,
            )
        else
            VerticalMainUI(
                pulse = pulse.value,
                time = time.value,
                battery = battery,
                pauseIcon = icons[iconNdx],
                topButtonPadding = topButtonPadding,
                onReset = reset,
                onTogglePause = togglePause,
            )
        // endregion
    }
}

@Composable
private fun VerticalMainUI(
    pulse: String,
    time: String,
    battery: String,
    pauseIcon: ImageVector,
    topButtonPadding: Dp,
    onReset: () -> Unit,
    onTogglePause: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = topButtonPadding + ButtonDefaults.MinHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // fill = false: the block may not grow past the space left by the buttons, but it
        // does not claim all of it either, so the buttons stay right under the digits.
        DigitsBlock(pulse, time, Modifier.weight(1f, fill = false))
        Row{
            val mdf = Modifier
                .padding(horizontal = 32.dp, vertical = 6.dp)
            ElevatedButton(
                modifier = mdf,
                onClick = onReset
            ){
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = "Reset")
            }
            ElevatedButton(
                modifier = mdf,
                onClick = onTogglePause
            ){
                Icon(
                    pauseIcon,
                    contentDescription = "Pause")
            }
        }
        BatteryIndicator(battery)
    }
}

@Composable
private fun HorizontalMainUI(
    pulse: String,
    time: String,
    pauseIcon: ImageVector,
    onReset: () -> Unit,
    onTogglePause: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        ElevatedButton(
            modifier = Modifier.padding(start = 8.dp),
            onClick = onReset
        ){
            Icon(
                Icons.Default.RestartAlt,
                contentDescription = "Reset")
        }
        // In landscape the digits are the star of the show: they claim every pixel of
        // height we can spare and sit at the top of that space. fill = false keeps the
        // box only as wide as the digits, so the buttons hug them instead of the edges.
        DigitsBlock(
            pulse,
            time,
            Modifier
                .weight(1f, fill = false)
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.TopCenter,
            bottomPaddingOfTimeHeight = 0.5f,
        )
        ElevatedButton(
            modifier = Modifier.padding(end = 8.dp),
            onClick = onTogglePause
        ){
            Icon(
                pauseIcon,
                contentDescription = "Pause")
        }
    }
}

// A pulse is a heart rate, so it never needs more than 3 digits; the probe uses the
// widest of them, so the digits always fill whatever space the layout leaves them.
private const val REF_SIZE_SP = 100f
private const val PULSE_PROBE = "888"
private const val TIME_PROBE = "0:00:00"
private const val TIME_TO_PULSE_RATIO = 0.5f

private class Probe(
    val width: Float,
    val timeWidth: Float,
    val pulseHeight: Float,
    val timeHeight: Float,
)

@Composable
private fun rememberProbe(): Probe {
    val measurer = rememberTextMeasurer()
    return remember(measurer) {
        val style = TextStyle(fontSize = REF_SIZE_SP.sp)
        val pulse = measurer.measure(PULSE_PROBE, style).size
        val time = measurer.measure(TIME_PROBE, style).size
        Probe(
            width = pulse.width.toFloat(),
            timeWidth = time.width.toFloat(),
            pulseHeight = pulse.height.toFloat(),
            timeHeight = time.height.toFloat(),
        )
    }
}

// Text scales linearly, so one measurement at a reference size is enough to compute
// the largest size that fits both the width and the height we have been given.
@Composable
private fun DigitsBlock(
    pulse: String,
    time: String,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    // Fraction of the rendered timer height to leave empty below the digits.
    bottomPaddingOfTimeHeight: Float = 0f,
) {
    val probe = rememberProbe()
    BoxWithConstraints(modifier, contentAlignment = contentAlignment) {
        val density = LocalDensity.current
        val availableWidth = with(density) { maxWidth.toPx() }
        val availableHeight = with(density) { maxHeight.toPx() }
        // The bottom padding is a fraction of the timer's own height, which in turn
        // depends on the scale we are solving for. Folding that fraction into the height
        // budget resolves the circularity:
        //   scale = height / (pulseHeight + (1 + k) * TIME_TO_PULSE_RATIO * timeHeight)
        val heightBudget = probe.pulseHeight +
                (1f + bottomPaddingOfTimeHeight) * TIME_TO_PULSE_RATIO * probe.timeHeight
        val pulseSize = REF_SIZE_SP * min(
            availableWidth / probe.width,
            availableHeight / heightBudget,
        )
        // The block always takes the width of the widest value we can ever show, so the
        // neighbours do not move when the current value gets shorter.
        val widest = max(probe.width, probe.timeWidth * TIME_TO_PULSE_RATIO) * pulseSize / REF_SIZE_SP
        val bottomPadding = bottomPaddingOfTimeHeight * TIME_TO_PULSE_RATIO *
                probe.timeHeight * pulseSize / REF_SIZE_SP
        Column(
            modifier = Modifier
                .width(with(density) { widest.toDp() })
                .padding(bottom = with(density) { bottomPadding.toDp() }),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = pulse, fontSize = pulseSize.sp)
            Text(text = time, fontSize = (pulseSize * TIME_TO_PULSE_RATIO).sp)
        }
    }
}

@Composable
private fun BatteryIndicator(battery: String) {
    AnimatedVisibility(
        visible = battery.isNotEmpty(),
        modifier = Modifier.padding( 6.dp ),
        enter = fadeIn(animationSpec = tween(2000))
    ){
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.BatteryStd,
                contentDescription = "BLE device battery charge level",
                modifier = Modifier
                    .size(16.spToDp)
            )
            Text(text = battery, fontSize = 16.sp)
        }
    }
}

@Preview(
    showBackground = true,
    heightDp = 600,
    widthDp = 300)
@Composable
fun DefaultPreview() {
    PulsarTheme {
        MainUI()
    }
}

@Preview(
    showBackground = true,
    heightDp = 300,
    widthDp = 600)
@Composable
fun LandscapePreview() {
    PulsarTheme {
        MainUI()
    }
}