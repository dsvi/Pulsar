package com.ds.pulsar

import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Rational
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.app.PictureInPictureParamsCompat
import androidx.core.util.Consumer
import com.ds.pulsar.ui.theme.PulsarTheme

// The system rejects any aspect ratio outside [100/239, 239/100], so the digits' own
// ratio has to be clamped before it is offered as the shape of the PiP window.
private const val MIN_ASPECT = 100f / 239f
private const val MAX_ASPECT = 239f / 100f

private fun aspectRatioOf(width: Int, height: Int): Rational {
    val aspect = (width.toFloat() / height.toFloat()).coerceIn(MIN_ASPECT, MAX_ASPECT)
    return Rational((aspect * 1000f).toInt(), 1000)
}

// Mirrors the activity's picture-in-picture mode into observable state, so the Compose
// tree can swap the full UI for the compact one while the window is small.
// The activity is nullable because previews compose this without one.
internal class PictureInPictureState(
    private val activity: MainActivity?,
) {
    // The button is hidden only when we positively know the device cannot do
    // picture-in-picture; a preview has no activity and should look like the real thing.
    val supported: Boolean =
        activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            ?: true

    var isInPictureInPicture by mutableStateOf(activity?.isInPictureInPictureMode ?: false)
        private set

    // Where the digits currently sit, reported by the composable that draws them. Not
    // observable on purpose: it is only read at the moment we ask to enter PiP, to tell
    // the system which part of the screen to grow into the floating window.
    var digitsBounds: LayoutCoordinates? = null

    internal val modeChangedListener =
        Consumer<PictureInPictureModeChangedInfo> {
            isInPictureInPicture = it.isInPictureInPictureMode
        }

    fun enter() {
        val act = activity ?: return
        val builder = PictureInPictureParamsCompat.Builder()
        digitsBounds?.takeIf { it.isAttached }?.let { coordinates ->
            val size = coordinates.size
            builder.setAspectRatio(aspectRatioOf(size.width, size.height))
            val (left, top, right, bottom) = coordinates.boundsInWindow()
            builder.setSourceRectHint(
                Rect(
                    left.toInt(), top.toInt(),
                    right.toInt(), bottom.toInt(),
                ),
            )
        }
        val params = builder.build()
        // Setting the params as well lets the system reuse them when the user leaves the
        // app by other means (home button, gesture) instead of pressing the button.
        act.setPictureInPictureParams(params)
        act.enterPictureInPictureMode(params)
    }
}

@Composable
internal fun rememberPictureInPictureState(): PictureInPictureState {
    val act = LocalActivity.current as? MainActivity
    val state = remember(act) { PictureInPictureState(act) }
    DisposableEffect(state) {
        if (act != null) {
            act.addOnPictureInPictureModeChangedListener(state.modeChangedListener)
        }
        onDispose {
            if (act != null) {
                act.removeOnPictureInPictureModeChangedListener(state.modeChangedListener)
            }
        }
    }
    return state
}

// The whole PiP window is the readout and nothing else: the system draws its own
// controls over the window, so buttons and the battery would only crowd the digits.
// The timer keeps running because its state and collectors live in MainUI, above this.
@Composable
internal fun PictureInPictureUI(
    pulse: String,
    time: String,
    onDigitsBounds: (LayoutCoordinates) -> Unit = {},
) {
    DigitsBlock(
        pulse = pulse,
        time = time,
        modifier = Modifier.fillMaxSize(),
        onDigitsBounds = onDigitsBounds,
    )
}

@Preview(
    showBackground = true,
    heightDp = 120,
    widthDp = 120,
)
@Composable
fun PictureInPicturePreview() {
    PulsarTheme {
        PictureInPictureUI("172", "1:23:45")
    }
}
