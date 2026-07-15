package com.andrii.patephone.action

sealed class PlayerAction {
    object PlayPause : PlayerAction()
    object SkipNext : PlayerAction()
    object SkipPrevious : PlayerAction()
    object ToggleShuffle : PlayerAction()
    object ToggleRepeat : PlayerAction()
    object AddToFavs : PlayerAction()
}
