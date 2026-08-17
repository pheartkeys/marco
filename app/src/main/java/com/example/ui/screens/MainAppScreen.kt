package com.example.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.viewmodel.TravelViewModel

object AppRoutes {
    const val CHAT = "chat"
    const val ONBOARDING = "onboarding"
    const val SETTINGS = "settings"
    const val WALLET = "wallet"
    const val LOYALTY_MANAGEMENT = "loyalty_management"
    const val ITINERARY = "itinerary"
    const val SAFETY_MAP = "safety_map"
    const val MEMORIES = "memories"
    const val VENDOR_CALL = "vendor_call"
    const val PREFERENCES = "preferences"
    const val PLAN_TRIP = "plan_trip"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val FORGOT_PASSWORD = "forgot_password"
}

@Composable
fun MainAppScreen(
    viewModel: TravelViewModel = viewModel()
) {
    val navController = rememberNavController()
    var vendorCallTarget by remember {
        mutableStateOf("Lodging Front Desk" to "Confirm reservation details and accessibility accommodations")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = AppRoutes.CHAT,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350)
                ) + fadeIn(animationSpec = tween(350))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350)
                ) + fadeOut(animationSpec = tween(350))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350)
                ) + fadeIn(animationSpec = tween(350))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350)
                ) + fadeOut(animationSpec = tween(350))
            }
        ) {
            // Guided Onboarding Setup Wizard
            composable(route = AppRoutes.ONBOARDING) {
                OnboardingWizardScreen(
                    viewModel = viewModel,
                    onOnboardingComplete = {
                        navController.navigate(AppRoutes.CHAT) {
                            popUpTo(AppRoutes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            // Main Chat Screen
            composable(route = AppRoutes.CHAT) {
                ConciergeChatScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
                    onOpenWallet = { navController.navigate(AppRoutes.WALLET) },
                    onOpenItinerary = { navController.navigate(AppRoutes.ITINERARY) },
                    onOpenSafetyMap = { navController.navigate(AppRoutes.SAFETY_MAP) },
                    onOpenMemories = { navController.navigate(AppRoutes.MEMORIES) },
                    onOpenVendorCall = { vendor, question ->
                        vendorCallTarget = vendor to question
                        navController.navigate(AppRoutes.VENDOR_CALL)
                    },
                    onOpenPreferences = { navController.navigate(AppRoutes.PREFERENCES) },
                    onOpenPlanTrip = { navController.navigate(AppRoutes.PLAN_TRIP) },
                    onOpenAuth = { navController.navigate(AppRoutes.SIGN_IN) },
                    onOpenOnboarding = { navController.navigate(AppRoutes.ONBOARDING) }
                )
            }

            // Dedicated Sign In Screen
            composable(route = AppRoutes.SIGN_IN) {
                SignInScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onNavigateToSignUp = {
                        navController.navigate(AppRoutes.SIGN_UP)
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(AppRoutes.FORGOT_PASSWORD)
                    },
                    onAuthSuccess = {
                        navController.popBackStack()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Dedicated Sign Up Screen
            composable(route = AppRoutes.SIGN_UP) {
                SignUpScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onNavigateToSignIn = {
                        navController.navigate(AppRoutes.SIGN_IN) {
                            popUpTo(AppRoutes.SIGN_IN) { inclusive = true }
                        }
                    },
                    onAuthSuccess = {
                        navController.navigate(AppRoutes.ONBOARDING) {
                            popUpTo(AppRoutes.SIGN_UP) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Dedicated Forgot Password Screen
            composable(route = AppRoutes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onNavigateToSignIn = {
                        navController.popBackStack()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Settings Screen
            composable(route = AppRoutes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToAuth = { navController.navigate(AppRoutes.SIGN_IN) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Wallet & Rewards Screen
            composable(route = AppRoutes.WALLET) {
                WalletRewardsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Loyalty Account Management
            composable(route = AppRoutes.LOYALTY_MANAGEMENT) {
                WalletRewardsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Detailed Itinerary Screen
            composable(route = AppRoutes.ITINERARY) {
                ItineraryDetailScreen(
                    viewModel = viewModel,
                    onOpenPlanDialog = { navController.navigate(AppRoutes.PLAN_TRIP) },
                    onNavigateToVendorCall = { vendor, question ->
                        vendorCallTarget = vendor to question
                        navController.navigate(AppRoutes.VENDOR_CALL)
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Offline Safety & GPS Map Screen
            composable(route = AppRoutes.SAFETY_MAP) {
                OfflineMapSafetyScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Group Memories & Story Reel Screen
            composable(route = AppRoutes.MEMORIES) {
                GroupMemoriesScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // AI Vendor Telephony Call Screen
            composable(route = AppRoutes.VENDOR_CALL) {
                VendorCallScreen(
                    viewModel = viewModel,
                    initialVendor = vendorCallTarget.first,
                    initialQuestion = vendorCallTarget.second,
                    modifier = Modifier.fillMaxSize(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Traveler DNA & Preferences Studio Screen
            composable(route = AppRoutes.PREFERENCES) {
                TravelerPreferenceScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onNavigateBack = { navController.popBackStack() },
                    onSelectProactiveTrip = { _ ->
                        navController.navigate(AppRoutes.PLAN_TRIP)
                    }
                )
            }

            // AI Custom Trip Builder Screen
            composable(route = AppRoutes.PLAN_TRIP) {
                PlanTripScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onNavigateBack = { navController.popBackStack() },
                    onTripCreated = { _ ->
                        navController.navigate(AppRoutes.ITINERARY) {
                            popUpTo(AppRoutes.CHAT)
                        }
                    }
                )
            }
        }
    }
}
