package com.app.aisheildme.bytrix.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.app.aisheildme.bytrix.feature.home.ShieldAction
import com.app.aisheildme.bytrix.tier1_ui.activity.ActivityScreen
import com.app.aisheildme.bytrix.tier1_ui.dailyassistant.DailyAssistantScreen
import com.app.aisheildme.bytrix.tier1_ui.documents.DocumentImportScreen
import com.app.aisheildme.bytrix.tier1_ui.vault.PersonalVaultScreen
import com.app.aisheildme.bytrix.tier3_negotiator.FollowUp
import com.app.aisheildme.bytrix.feature.auth.basicinfo.BasicInfoScreen
import com.app.aisheildme.bytrix.feature.auth.login.LoginScreen
import com.app.aisheildme.bytrix.feature.auth.onboarding.OnboardingScreen
import com.app.aisheildme.bytrix.feature.auth.resetpassword.ResetPasswordScreen
import com.app.aisheildme.bytrix.feature.auth.setupprofile.SetupProfileScreen
import com.app.aisheildme.bytrix.feature.auth.signup.SignupScreen
import com.app.aisheildme.bytrix.feature.auth.splash.SplashScreen
import com.app.aisheildme.bytrix.feature.auth.vaultpin.CreateVaultPinScreen
import com.app.aisheildme.bytrix.feature.auth.verifyemail.VerifyEmailScreen
import com.app.aisheildme.bytrix.core.common.DetailHandoff
import com.app.aisheildme.bytrix.core.util.FileOpener
import com.app.aisheildme.bytrix.feature.banking.BankStatementDetailScreen
import com.app.aisheildme.bytrix.feature.banking.BankingScreen
import com.app.aisheildme.bytrix.feature.bookkeeper.AddInvoiceScreen
import com.app.aisheildme.bytrix.feature.checkin.CheckInFormScreen
import com.app.aisheildme.bytrix.feature.checkin.CheckInPreviewScreen
import com.app.aisheildme.bytrix.feature.checkin.CheckInReportScreen
import com.app.aisheildme.bytrix.feature.checkinhistory.CheckInHistoryScreen
import com.app.aisheildme.bytrix.feature.dashboard.DashboardScreen
import com.app.aisheildme.bytrix.feature.documents.DocumentsScreen
import com.app.aisheildme.bytrix.feature.folders.FoldersScreen
import com.app.aisheildme.bytrix.feature.healthcare.AppointmentDetailScreen
import com.app.aisheildme.bytrix.feature.healthcare.HealthcareScreen
import com.app.aisheildme.bytrix.feature.home.Industry
import com.app.aisheildme.bytrix.feature.notification.NotificationScreen
import com.app.aisheildme.bytrix.feature.profile.AvailabilityScreen
import com.app.aisheildme.bytrix.feature.profile.EditProfileScreen
import com.app.aisheildme.bytrix.feature.profile.ResetPinScreen
import com.app.aisheildme.bytrix.feature.profile.StaticPageScreen
import com.app.aisheildme.bytrix.feature.retail.RetailDetailScreen
import com.app.aisheildme.bytrix.feature.retail.RetailScreen
import com.app.aisheildme.bytrix.feature.share.ShareScreen
import com.app.aisheildme.bytrix.feature.vault.VaultGateDialog
import com.app.aisheildme.bytrix.feature.vault.VaultGateViewModel

@Composable
fun AppNavHost(
    handoff: DetailHandoff,
    fileOpener: FileOpener,
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        appViewModel.sessionEnded.collect {
            navController.navigateAndClear(LoginRoute)
        }
    }

    NavHost(navController = navController, startDestination = SplashRoute) {

        composable<SplashRoute> {
            SplashScreen(
                onOnboarding = { navController.navigateReplacing(OnboardingRoute) },
                onLogin = { navController.navigateReplacing(LoginRoute) },
                onCreateVaultPin = { navController.navigateAndClear(CreateVaultPinRoute) },
                onDashboard = { navController.navigateAndClear(DashboardRoute) },
            )
        }

        composable<OnboardingRoute> {
            OnboardingScreen(
                onFinished = { navController.navigateReplacing(LoginRoute) },
            )
        }

        composable<LoginRoute> {
            LoginScreen(
                onAuthenticated = { user, email ->
                    AuthFlow.routeAfterAuth(navController, user, email)
                },
                onVerifyOtp = { email, flow ->
                    navController.navigate(VerifyEmailRoute(email, flow))
                },
                onSignup = { navController.navigateReplacing(SignupRoute) },
            )
        } 

        composable<SignupRoute> {
            SignupScreen(
                onContinue = { account ->
                    navController.navigate(
                        BasicInfoRoute(
                            firstName = account.firstName,
                            lastName = account.lastName,
                            email = account.email,
                            password = account.password,
                            confirmPassword = account.confirmPassword,
                        ),
                    )
                },
                onLogin = { navController.navigateReplacing(LoginRoute) },
            )
        }

        composable<BasicInfoRoute> {
            BasicInfoScreen(
                onRegistered = { email ->
                    navController.navigate(VerifyEmailRoute(email, VerifyFlow.Signup))
                },
            )
        }

        composable<VerifyEmailRoute> {
            VerifyEmailScreen(
                onVerified = { user -> AuthFlow.routeAfterOtpVerified(navController, user) },
                onResetToken = { token ->
                    navController.navigateReplacing(ResetPasswordRoute(token))
                },
            )
        }

        composable<SetupProfileRoute> {
            SetupProfileScreen(
                onDashboard = { navController.navigateAndClear(DashboardRoute) },
                onCreateVaultPin = { navController.navigateReplacing(CreateVaultPinRoute) },
            )
        }

        composable<CreateVaultPinRoute> {
            CreateVaultPinScreen(
                onPinSet = { navController.navigateAndClear(DashboardRoute) },
            )
        }

        composable<ResetPasswordRoute> {
            ResetPasswordScreen(
                onDone = { navController.navigateAndClear(LoginRoute) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<DashboardRoute> {
            val vaultViewModel: VaultGateViewModel = hiltViewModel()
            val vaultState by vaultViewModel.state.collectAsStateWithLifecycle()

            DashboardScreen(
                onOpenFolder = { category ->
                    vaultViewModel.requestUnlock {
                        navController.navigate(
                            DocumentsRoute(
                                folderName = category.name,
                                categoryId = category.id,
                            ),
                        )
                    }
                },
                onSeeAllFolders = { navController.navigate(FoldersRoute()) },
                onNotifications = { navController.navigate(NotificationRoute) },
                onIndustry = { industry ->
                    navController.navigate(
                        when (industry) {
                            Industry.Retail -> RetailRoute
                            Industry.Banking -> BankingRoute
                            Industry.Healthcare -> HealthcareRoute
                        },
                    )
                },
                onAddFolder = { navController.navigate(FoldersRoute(openCreate = true)) },
                onEditProfile = { navController.navigate(EditProfileRoute) },
                onPrivacyPolicy = { navController.navigate(StaticPageRoute()) },
                onChangePassword = {
                    navController.navigate(ResetPasswordRoute(token = null))
                },
                onResetPin = { navController.navigate(ResetPinRoute) },
                onAvailability = { navController.navigate(AvailabilityRoute) },
                onCheckInHistory = { navController.navigate(CheckInHistoryRoute) },
                onLoggedOut = { navController.navigateAndClear(LoginRoute) },
                onShieldAction = { action ->
                    // Personal data screens sit behind the vault PIN / biometric gate.
                    vaultViewModel.requestUnlock {
                        navController.navigate(
                            when (action) {
                                ShieldAction.Assistant -> AssistantRoute
                                ShieldAction.Vault -> PersonalVaultRoute
                                ShieldAction.ImportDocument -> DocumentImportRoute
                                ShieldAction.Activity -> ActivityRoute(tab = 0)
                                ShieldAction.Consents -> ActivityRoute(tab = 1)
                            },
                        )
                    }
                },
            )

            VaultGateDialog(
                state = vaultState,
                onPinChange = vaultViewModel::onPinChange,
                onPinSubmit = vaultViewModel::onPinSubmit,
                onBiometricSuccess = vaultViewModel::onBiometricSuccess,
                onBiometricPrompted = vaultViewModel::onBiometricPrompted,
                onDismiss = vaultViewModel::onDismiss,
            )
        }

        composable<FoldersRoute> {
            val vaultViewModel: VaultGateViewModel = hiltViewModel()
            val vaultState by vaultViewModel.state.collectAsStateWithLifecycle()

            FoldersScreen(
                onBack = navController::popBackStack,
                onOpenFolder = { category ->
                    vaultViewModel.requestUnlock {
                        navController.navigate(
                            DocumentsRoute(
                                folderName = category.name,
                                categoryId = category.id,
                            ),
                        )
                    }
                },
            )

            VaultGateDialog(
                state = vaultState,
                onPinChange = vaultViewModel::onPinChange,
                onPinSubmit = vaultViewModel::onPinSubmit,
                onBiometricSuccess = vaultViewModel::onBiometricSuccess,
                onBiometricPrompted = vaultViewModel::onBiometricPrompted,
                onDismiss = vaultViewModel::onDismiss,
            )
        }

        composable<DocumentsRoute> {
            DocumentsScreen(
                onBack = navController::popBackStack,
                onShare = { document ->
                    navController.navigate(
                        ShareRoute(documentId = document.id, title = document.title),
                    )
                },
            )
        }

        composable<ShareRoute> {
            ShareScreen(
                onBack = navController::popBackStack,
                onShared = { navController.popBackStack() },
            )
        }

        composable<NotificationRoute> {
            NotificationScreen(onBack = navController::popBackStack)
        }

        composable<HealthcareRoute> {
            HealthcareScreen(
                onBack = navController::popBackStack,
                onOpenAppointment = { appointment ->
                    handoff.put(DetailHandoff.APPOINTMENT, appointment)
                    navController.navigate(AppointmentDetailRoute)
                },
                onCheckIn = { navController.navigate(CheckInFormRoute) },
                onCheckInHistory = { navController.navigate(CheckInHistoryRoute) },
            )
        }

        composable<AppointmentDetailRoute> {
            AppointmentDetailScreen(
                appointment = handoff.get(DetailHandoff.APPOINTMENT),
                onBack = navController::popBackStack,
            )
        }

        composable<RetailRoute> {
            RetailScreen(
                onBack = navController::popBackStack,
                onOpenOrder = { order ->
                    handoff.put(DetailHandoff.RETAIL_ORDER, order)
                    navController.navigate(RetailDetailRoute)
                },
            )
        }

        composable<RetailDetailRoute> {
            RetailDetailScreen(
                order = handoff.get(DetailHandoff.RETAIL_ORDER),
                onBack = navController::popBackStack,
            )
        }

        composable<BankingRoute> {
            BankingScreen(
                onBack = navController::popBackStack,
                onOpenStatement = { statement ->
                    handoff.put(DetailHandoff.BANK_STATEMENT, statement)
                    navController.navigate(BankStatementDetailRoute)
                },
            )
        }

        composable<BankStatementDetailRoute> {
            BankStatementDetailScreen(
                statement = handoff.get(DetailHandoff.BANK_STATEMENT),
                fileOpener = fileOpener,
                onBack = navController::popBackStack,
            )
        }

        composable<CheckInHistoryRoute> {
            CheckInHistoryScreen(onBack = navController::popBackStack)
        }

        composable<AddInvoiceRoute> {
            AddInvoiceScreen(onBack = navController::popBackStack)
        }

        composable<EditProfileRoute> {
            EditProfileScreen(onBack = navController::popBackStack)
        }

        composable<ResetPinRoute> {
            ResetPinScreen(onBack = navController::popBackStack)
        }

        composable<AvailabilityRoute> {
            AvailabilityScreen(onBack = navController::popBackStack)
        }

        composable<StaticPageRoute> {
            StaticPageScreen(onBack = navController::popBackStack)
        }

        composable<CheckInFormRoute> {
            CheckInFormScreen(
                onBack = navController::popBackStack,
                onPreview = { navController.navigate(CheckInPreviewRoute) },
            )
        }

        composable<CheckInPreviewRoute> {
            CheckInPreviewScreen(
                onBack = navController::popBackStack,
                onReport = { navController.navigate(CheckInReportRoute) },
            )
        }

        composable<AssistantRoute> {
            DailyAssistantScreen(
                onBack = navController::popBackStack,
                onFollowUp = { follow ->
                    navController.navigate(
                        when (follow) {
                            FollowUp.OPEN_DOCUMENT_IMPORT -> DocumentImportRoute
                            FollowUp.OPEN_CONSENTS -> ActivityRoute(tab = 1)
                            FollowUp.OPEN_VAULT -> PersonalVaultRoute
                            FollowUp.OPEN_ACTIVITY -> ActivityRoute(tab = 0)
                            FollowUp.OPEN_CHECK_IN_FORM -> CheckInFormRoute
                        },
                    )
                },
            )
        }

        composable<PersonalVaultRoute> {
            PersonalVaultScreen(
                onBack = navController::popBackStack,
                onImportDocument = { navController.navigate(DocumentImportRoute) },
            )
        }

        composable<DocumentImportRoute> {
            DocumentImportScreen(onBack = navController::popBackStack)
        }

        composable<ActivityRoute> { entry ->
            ActivityScreen(
                onBack = navController::popBackStack,
                startTab = entry.toRoute<ActivityRoute>().tab,
            )
        }

        composable<CheckInReportRoute> {
            CheckInReportScreen(
                onDone = {
                    navController.popBackStack(HealthcareRoute, inclusive = false)
                },
            )
        }
    }
}
