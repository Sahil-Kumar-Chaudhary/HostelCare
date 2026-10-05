package com.hostelcare.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val STUDENT_HOME = "student_home"
    const val NEW_COMPLAINT = "new_complaint"
    const val AI_REVIEW = "ai_review"
    const val MY_COMPLAINTS = "my_complaints"
    const val COMPLAINT_DETAILS = "complaint_details/{complaintId}"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val CHANGE_PASSWORD = "change_password"
    const val FEEDBACK = "feedback/{complaintId}"

    const val ADMIN_LOGIN = "admin_login"
    const val ADMIN_HOME = "admin_home"
    const val ADMIN_COMPLAINTS = "admin_complaints"
    const val ADMIN_COMPLAINT_DETAILS = "admin_complaint_details/{complaintId}"
    const val ASSIGN_STAFF = "assign_staff/{complaintId}"
    const val RESOLVE_COMPLAINT = "resolve_complaint/{complaintId}"

    fun complaintDetails(id: String) = "complaint_details/$id"
    fun feedback(id: String) = "feedback/$id"
    fun adminComplaintDetails(id: String) = "admin_complaint_details/$id"
    fun assignStaff(id: String) = "assign_staff/$id"
    fun resolveComplaint(id: String) = "resolve_complaint/$id"
}

@Composable
fun HostelCareAppNavHost(
    navController: NavHostController,
    startDestination: String = Routes.SPLASH,
    appContainer: com.hostelcare.app.HostelCareApp
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SPLASH) {
            com.hostelcare.app.ui.screens.auth.SplashScreen(navController, appContainer)
        }
        composable(Routes.LOGIN) {
            com.hostelcare.app.ui.screens.auth.LoginScreen(navController, appContainer)
        }
        composable(Routes.SIGN_UP) {
            com.hostelcare.app.ui.screens.auth.SignUpScreen(navController, appContainer)
        }
        composable(Routes.STUDENT_HOME) {
            com.hostelcare.app.ui.screens.student.StudentHomeScreen(navController, appContainer)
        }
        composable(Routes.NEW_COMPLAINT) {
            com.hostelcare.app.ui.screens.student.NewComplaintScreen(navController, appContainer)
        }
        composable(Routes.AI_REVIEW) {
            com.hostelcare.app.ui.screens.student.AiReviewScreen(navController, appContainer)
        }
        composable(Routes.MY_COMPLAINTS) {
            com.hostelcare.app.ui.screens.student.MyComplaintsScreen(navController, appContainer)
        }
        composable(Routes.COMPLAINT_DETAILS) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("complaintId") ?: ""
            com.hostelcare.app.ui.screens.student.ComplaintDetailsScreen(navController, appContainer, id)
        }
        composable(Routes.NOTIFICATIONS) {
            com.hostelcare.app.ui.screens.student.NotificationsScreen(navController, appContainer)
        }
        composable(Routes.PROFILE) {
            com.hostelcare.app.ui.screens.student.ProfileScreen(navController, appContainer)
        }
        composable(Routes.EDIT_PROFILE) {
            com.hostelcare.app.ui.screens.student.EditProfileScreen(navController, appContainer)
        }
        composable(Routes.CHANGE_PASSWORD) {
            com.hostelcare.app.ui.screens.student.ChangePasswordScreen(navController)
        }
        composable(Routes.FEEDBACK) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("complaintId") ?: ""
            com.hostelcare.app.ui.screens.student.FeedbackScreen(navController, appContainer, id)
        }
        composable(Routes.ADMIN_LOGIN) {
            com.hostelcare.app.ui.screens.auth.AdminLoginScreen(navController, appContainer)
        }
        composable(Routes.ADMIN_HOME) {
            com.hostelcare.app.ui.screens.admin.AdminHomeScreen(navController, appContainer)
        }
        composable(Routes.ADMIN_COMPLAINTS) {
            com.hostelcare.app.ui.screens.admin.AdminComplaintListScreen(navController, appContainer)
        }
        composable(Routes.ADMIN_COMPLAINT_DETAILS) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("complaintId") ?: ""
            com.hostelcare.app.ui.screens.admin.AdminComplaintDetailsScreen(navController, appContainer, id)
        }
        composable(Routes.ASSIGN_STAFF) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("complaintId") ?: ""
            com.hostelcare.app.ui.screens.admin.AssignStaffScreen(navController, appContainer, id)
        }
        composable(Routes.RESOLVE_COMPLAINT) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("complaintId") ?: ""
            com.hostelcare.app.ui.screens.admin.ResolveComplaintScreen(navController, appContainer, id)
        }
    }
}
