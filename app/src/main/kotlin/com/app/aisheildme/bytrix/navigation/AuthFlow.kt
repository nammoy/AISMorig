package com.app.aisheildme.bytrix.navigation

import androidx.navigation.NavController
import com.app.aisheildme.bytrix.domain.model.User

object AuthFlow {

    fun routeAfterAuth(navController: NavController, user: User, email: String) {
        when {
            !user.isEmailVerified ->
                navController.navigate(VerifyEmailRoute(email, VerifyFlow.Signup))

            !user.isPinSet -> navController.navigateAndClear(CreateVaultPinRoute)

            else -> navController.navigateAndClear(DashboardRoute)
        }
    }

    fun routeAfterOtpVerified(navController: NavController, user: User) {
        when {
            !user.hasProfileSetup -> navController.navigate(SetupProfileRoute) {
                popUpTo<VerifyEmailRoute> { inclusive = true }
            }
            user.isPinSet -> navController.navigateAndClear(DashboardRoute)
            else -> navController.navigate(CreateVaultPinRoute) {
                popUpTo<VerifyEmailRoute> { inclusive = true }
            }
        }
    }
}

fun NavController.navigateAndClear(destination: Destination) {
    navigate(destination) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

fun NavController.navigateReplacing(destination: Destination) {
    val current = currentBackStackEntry?.destination?.id
    navigate(destination) {
        if (current != null) popUpTo(current) { inclusive = true }
        launchSingleTop = true
    }
}
