package com.hostelcare.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Routes {
    const val SPLASH = "splash"
    const val PORTAL_SELECTION = "portal_selection"
    const val LOGIN = "login"
    const val WARDEN_LOGIN = "warden_login"
    const val WARDEN_HOME = "warden_home"
    const val WARDEN_DASHBOARD = "warden_home"
    const val STAFF_LOGIN = "staff_login"
    const val SIGN_UP = "signup"
    const val STUDENT_HOME = "student_home"
    const val NEW_COMPLAINT = "new_complaint"
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
    const val ADMIN_PROFILE = "admin_profile"
    const val ADMIN_COMPLAINT_DETAILS = "admin_complaint_details/{complaintId}"
    const val ASSIGN_STAFF = "assign_staff/{complaintId}"
    const val RESOLVE_COMPLAINT = "resolve_complaint/{complaintId}"

    // Warden Routes (Steps 1 - 10)
    const val WARDEN_COMPLAINTS = "warden_complaints"
    const val WARDEN_COMPLAINT_DETAILS = "warden_complaint_details/{complaintId}"
    const val WARDEN_ASSIGN_STAFF = "warden_assign_staff/{complaintId}"
    const val WARDEN_STAFF = "warden_staff"
    const val WARDEN_ADD_STAFF = "warden_add_staff"
    const val WARDEN_STAFF_PROFILE = "warden_staff_profile/{staffId}"
    const val WARDEN_REPORTS = "warden_reports"
    const val WARDEN_PROFILE = "warden_profile"
    const val WARDEN_NOTIFICATIONS = "warden_notifications"
    const val WARDEN_MANAGEMENT = "warden_management"

    // Staff Portal Routes (Screens 1 - 8)
    const val STAFF_HOME = "staff_home"
    const val STAFF_TASKS = "staff_tasks"
    const val STAFF_TASK_DETAILS = "staff_task_details/{taskId}"
    const val STAFF_UPDATE_STATUS = "staff_update_status/{taskId}"
    const val STAFF_WORK_COMPLETED = "staff_work_completed/{taskId}"
    const val STAFF_NOTIFICATIONS = "staff_notifications"
    const val STAFF_PROFILE = "staff_profile"

    fun complaintDetails(id: String) = "complaint_details/$id"
    fun feedback(id: String) = "feedback/$id"
    fun adminComplaintDetails(id: String) = "admin_complaint_details/$id"
    fun assignStaff(id: String) = "assign_staff/$id"
    fun resolveComplaint(id: String) = "resolve_complaint/$id"

    fun wardenComplaintDetails(id: String) = "warden_complaint_details/$id"
    fun wardenAssignStaff(id: String) = "warden_assign_staff/$id"
    fun wardenStaffProfile(id: String) = "warden_staff_profile/$id"
    fun staffTaskDetails(id: String) = "staff_task_details/$id"
    fun staffUpdateStatus(id: String) = "staff_update_status/$id"
    fun staffWorkCompleted(id: String) = "staff_work_completed/$id"
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
        composable(Routes.PORTAL_SELECTION) {
            com.hostelcare.app.ui.screens.auth.PortalSelectionScreen(navController, appContainer)
        }
        composable(Routes.LOGIN) {
            com.hostelcare.app.ui.screens.auth.LoginScreen(navController, appContainer)
        }
        composable(Routes.WARDEN_LOGIN) {
            com.hostelcare.app.ui.screens.auth.WardenLoginScreen(navController, appContainer)
        }
        composable(Routes.WARDEN_HOME) {
            com.hostelcare.app.ui.screens.warden.WardenDashboardScreen(navController, appContainer)
        }
        composable(Routes.STAFF_LOGIN) {
            com.hostelcare.app.ui.screens.auth.StaffLoginScreen(navController, appContainer)
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

        composable(Routes.MY_COMPLAINTS) {
            com.hostelcare.app.ui.screens.student.MyComplaintsScreen(navController, appContainer)
        }
        composable(Routes.COMPLAINT_DETAILS) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("complaintId") ?: ""
            com.hostelcare.app.ui.screens.student.ComplaintDetailsScreen(navController, appContainer, id)
        }
        composable(Routes.NOTIFICATIONS) {
            val user by appContainer.repository.currentUserFlow().collectAsStateWithLifecycle(null)
            when (user?.role) {
                com.hostelcare.app.data.model.Role.WARDEN -> com.hostelcare.app.ui.screens.warden.WardenNotificationsScreen(navController, appContainer)
                com.hostelcare.app.data.model.Role.STAFF -> com.hostelcare.app.ui.screens.staff.StaffNotificationsScreen(navController, appContainer)
                else -> com.hostelcare.app.ui.screens.student.NotificationsScreen(navController, appContainer)
            }
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
        composable(Routes.ADMIN_PROFILE) {
            com.hostelcare.app.ui.screens.admin.AdminProfileScreen(navController, appContainer)
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

        // Warden Full Flow Screens (Screens 2 - 10)
        composable(Routes.WARDEN_COMPLAINTS) {
            com.hostelcare.app.ui.screens.warden.WardenComplaintsListScreen(navController, appContainer)
        }
        composable(Routes.WARDEN_COMPLAINT_DETAILS) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("complaintId") ?: ""
            com.hostelcare.app.ui.screens.warden.WardenComplaintDetailsScreen(navController, appContainer, id)
        }
        composable(Routes.WARDEN_ASSIGN_STAFF) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("complaintId") ?: ""
            com.hostelcare.app.ui.screens.warden.WardenAssignStaffScreen(navController, appContainer, id)
        }
        composable(Routes.WARDEN_STAFF) {
            com.hostelcare.app.ui.screens.warden.WardenStaffListScreen(navController, appContainer)
        }
        composable(Routes.WARDEN_ADD_STAFF) {
            com.hostelcare.app.ui.screens.warden.WardenAddStaffScreen(navController, appContainer)
        }
        composable(Routes.WARDEN_STAFF_PROFILE) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("staffId") ?: ""
            com.hostelcare.app.ui.screens.warden.WardenStaffProfileScreen(navController, appContainer, id)
        }
        composable(Routes.WARDEN_REPORTS) {
            com.hostelcare.app.ui.screens.warden.WardenReportsScreen(navController, appContainer)
        }
        composable(Routes.WARDEN_PROFILE) {
            com.hostelcare.app.ui.screens.warden.WardenProfileScreen(navController, appContainer)
        }
        composable(Routes.WARDEN_NOTIFICATIONS) {
            com.hostelcare.app.ui.screens.warden.WardenNotificationsScreen(navController, appContainer)
        }
        composable(Routes.WARDEN_MANAGEMENT) {
            com.hostelcare.app.ui.screens.warden.WardenManagementScreen(navController, appContainer)
        }

        // Staff Flow Screens (Screens 1 - 8)
        composable(Routes.STAFF_HOME) {
            com.hostelcare.app.ui.screens.staff.StaffHomeScreen(navController, appContainer)
        }
        composable(Routes.STAFF_TASKS) {
            com.hostelcare.app.ui.screens.staff.StaffTasksScreen(navController, appContainer)
        }
        composable(Routes.STAFF_TASK_DETAILS) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("taskId") ?: ""
            com.hostelcare.app.ui.screens.staff.StaffTaskDetailsScreen(navController, appContainer, id)
        }
        composable(Routes.STAFF_UPDATE_STATUS) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("taskId") ?: ""
            com.hostelcare.app.ui.screens.staff.StaffUpdateStatusScreen(navController, appContainer, id)
        }
        composable(Routes.STAFF_WORK_COMPLETED) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("taskId") ?: ""
            com.hostelcare.app.ui.screens.staff.StaffWorkCompletedScreen(navController, appContainer, id)
        }
        composable(Routes.STAFF_NOTIFICATIONS) {
            com.hostelcare.app.ui.screens.staff.StaffNotificationsScreen(navController, appContainer)
        }
        composable(Routes.STAFF_PROFILE) {
            com.hostelcare.app.ui.screens.staff.StaffProfileScreen(navController, appContainer)
        }
    }
}
