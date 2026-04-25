package com.andrii.patephone.action

import android.net.Uri

sealed class PlayerAction {
    object PlayPause : PlayerAction()
    object SkipNext : PlayerAction()
    object SkipPrevious : PlayerAction()
    object ToggleShuffle : PlayerAction()
    object ToggleRepeat : PlayerAction()
}
