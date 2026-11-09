package com.momento.core.navigation

object Routes {
    const val HOME = "home"
    const val DISCOVER = "discover"
    const val CREATE = "create"
    const val POD = "pod"
    const val PROFILE = "profile"
    const val AUTH = "auth"
}

object BottomDestinations {
    data class Destination(
        val route: String,
        val label: String
    )

    val items = listOf(
        Destination(Routes.HOME, "Home"),
        Destination(Routes.DISCOVER, "Discover"),
        Destination(Routes.CREATE, "Create"),
        Destination(Routes.POD, "Pod"),
        Destination(Routes.PROFILE, "Profile"),
    )
}
