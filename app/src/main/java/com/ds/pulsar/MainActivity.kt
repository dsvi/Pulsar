package com.ds.pulsar

import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.ds.pulsar.ui.theme.PulsarTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.concurrent.TimeoutException
import kotlin.math.abs


//val AppNavigator = staticCompositionLocalOf<NavHostController> {  }

private lateinit
var navController_: NavHostController
val navController by ::navController_

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PulsarTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    navController_ = rememberAnimatedNavController()
                    AnimatedNavHost(navController = navController, startDestination = Screens.main) {
                        val durationMs = 700
                        val animSpec = tween<IntOffset>(durationMs, easing = EaseInOutQuad)
                        composable(
                            Screens.main,
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentScope.SlideDirection.Left,
                                    animationSpec = animSpec
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentScope.SlideDirection.Right,
                                    animationSpec = animSpec
                                )
                            }) { MainScreen() }
                        composable(
                            Screens.devList,
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentScope.SlideDirection.Right,
                                    animationSpec = animSpec
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentScope.SlideDirection.Left,
                                    animationSpec = animSpec
                                )
                            }) { DeviceDiscoverScreen() }
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
        Icon(
            Icons.Default.Contactless,
            contentDescription = "List of available BLE devices",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(32.dp)
                .clickable { navController.navigate(Screens.devList) })
        val dm = LocalContext.current.resources.displayMetrics
        val pxPerSp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            100f,
            dm
        ) / 100f
        val heightInPx =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                maxHeight.value,
                dm
            )
        var spHeight = heightInPx / pxPerSp
        var biggerTextSize = 0f
        var smallerTextSize = 0f
        val recalcTextSize = {
            biggerTextSize = spHeight * 0.35f
            smallerTextSize = biggerTextSize * 0.5f
        }
        recalcTextSize()
        val bounds = Rect()
        val paint = Paint()
        val text = "0000"
        paint.textSize = biggerTextSize * pxPerSp
        paint.getTextBounds(text, 0, text.length, bounds)
        val textWidth = abs(bounds.right - bounds.left)
        val surfaceWidthInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            maxWidth.value,
            dm
        )
        if (surfaceWidthInPx < textWidth) {
            spHeight *= surfaceWidthInPx / textWidth
            recalcTextSize()
        }
        Column(
            Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val pulse = remember{ mutableStateOf("---") }
            val time = remember{ mutableStateOf("---") }
            var battery by remember{ mutableStateOf("") }
            LaunchedEffect(null ) {
                while (true) {
                    try {
                        coroutineScope {
                            println("collecting...")
                            pulse.value = "---"
                            heartRateStream.heartRateFlow().collect {
                                pulse.value = it.pulse.toString()
                                if (it.batteryLevel != null)
                                    battery = it.batteryLevel.toString() + "%"
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException)
                            throw e
                        if (e !is TimeoutException) {
                            println("Error collecting data from device: $e")
                            e.printStackTrace()
                        }
                    }
                    delay(200)
                }
            }
            val startTime = rememberSaveable{ System.currentTimeMillis() }
            LaunchedEffect(null ) {
                while (true) {
                    val td = (System.currentTimeMillis() - startTime) /1000
                    val h = td / 3600
                    val m = (td % 3600) / 60
                    val s = td % 60
                    if (h == 0L)
                        time.value = "%02d:%02d".format(m ,s)
                    else
                        time.value = "%d:%02d:%02d".format(h, m ,s)
                    delay(1000)
                }
            }
            Text(text = pulse.value, fontSize = biggerTextSize.sp)
            Text(text = time.value, fontSize = smallerTextSize.sp)
            AnimatedVisibility(
                visible = battery.isNotEmpty(),
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