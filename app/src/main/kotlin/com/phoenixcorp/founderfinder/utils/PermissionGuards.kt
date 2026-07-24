package com.phoenixcorp.founderfinder.ui.utils

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.permissions.UserPermissions
import kotlinx.coroutines.tasks.await

/**
 * Holds the current user's role and exposes canCreate / canUpdate helpers.
 * Resolve role once (or pass it in from a ViewModel) and reuse.
 */
data class UserPermissionState(
    val role: String?,
) {
    val isSpectator: Boolean get() = UserPermissions.isSpectator(role)
    val canView: Boolean get() = UserPermissions.canView(role)
    val canCreate: Boolean get() = UserPermissions.canCreate(role)
    val canUpdate: Boolean get() = UserPermissions.canUpdate(role)
    val canDelete: Boolean get() = UserPermissions.canDelete(role)
    val canSendMessage: Boolean get() = UserPermissions.canSendMessage(role)
    val canEngage: Boolean get() = UserPermissions.canEngage(role)
    val canComment: Boolean get() = UserPermissions.canComment(role)

    fun requireCreate(context: Context, actionLabel: String = "create"): Boolean {
        if (canCreate) return true
        Toast.makeText(
            context,
            UserPermissions.blockedActionMessage(actionLabel),
            Toast.LENGTH_SHORT
        ).show()
        return false
    }

    fun requireUpdate(context: Context, actionLabel: String = "edit"): Boolean {
        if (canUpdate) return true
        Toast.makeText(
            context,
            UserPermissions.blockedActionMessage(actionLabel),
            Toast.LENGTH_SHORT
        ).show()
        return false
    }

    fun requireEngage(context: Context, actionLabel: String = "engage"): Boolean {
        if (canEngage) return true
        Toast.makeText(
            context,
            UserPermissions.blockedActionMessage(actionLabel),
            Toast.LENGTH_SHORT
        ).show()
        return false
    }

    fun requireComment(context: Context): Boolean {
        if (canComment) return true
        Toast.makeText(
            context,
            UserPermissions.blockedActionMessage("comment"),
            Toast.LENGTH_SHORT
        ).show()
        return false
    }

    fun requireSendMessage(context: Context): Boolean {
        if (canSendMessage) return true
        Toast.makeText(
            context,
            UserPermissions.blockedActionMessage("send messages"),
            Toast.LENGTH_SHORT
        ).show()
        return false
    }
}

/**
 * Lightweight role fetch from Firestore profiles/{uid}.role
 * Prefer injecting role from a shared ProfileViewModel when you have one.
 */
suspend fun fetchCurrentUserRole(): String? {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
    return try {
        val snap = FirebaseFirestore.getInstance()
            .collection("profiles")
            .document(uid)
            .get()
            .await()
        snap.getString("role")
    } catch (_: Exception) {
        null
    }
}

/** Build a [UserPermissionState] from a known role string (e.g. from ViewModel). */
fun permissionsFor(role: String?): UserPermissionState = UserPermissionState(role)