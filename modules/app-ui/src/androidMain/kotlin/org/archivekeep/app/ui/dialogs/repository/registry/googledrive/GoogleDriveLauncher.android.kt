package org.archivekeep.app.ui.dialogs.repository.registry.googledrive

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import org.archivekeep.app.core.persistence.drivers.googledrive.GoogleDriveAuthException
import org.archivekeep.app.core.persistence.drivers.googledrive.GoogleDriveOAuthCredentials
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
actual fun createGoogleDriveAuthLauncher(
    onResult: (GoogleDriveAuthResult) -> Unit,
): (() -> Unit)? {
    val context = LocalContext.current.applicationContext
    val provider = LocalApplicationServices.current.googleDriveAuthProvider
    if (!provider.isSupported) return null
    val clientId = provider.clientId
    val coroutineScope = rememberCoroutineScope()
    val currentOnResult = rememberUpdatedState(onResult)
    val authorizationService = remember(context) { AuthorizationService(context) }

    DisposableEffect(authorizationService) {
        onDispose { authorizationService.dispose() }
    }

    val redirectUri = remember(clientId) { googleDriveRedirectUri(clientId) }
    val serviceConfiguration =
        remember {
            AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                Uri.parse("https://oauth2.googleapis.com/token"),
            )
        }

    val authorizationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val resultIntent = result.data
            val authorizationException = AuthorizationException.fromIntent(resultIntent)
            if (authorizationException != null) {
                if (
                    authorizationException.type == AuthorizationException.TYPE_GENERAL_ERROR &&
                    authorizationException.error == AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW.error
                ) {
                    currentOnResult.value(GoogleDriveAuthResult.Cancelled)
                } else {
                    Log.e("GoogleDriveAuth", "Google Drive authorization failed", authorizationException)
                    currentOnResult.value(GoogleDriveAuthResult.Failure(authorizationException))
                }
                return@rememberLauncherForActivityResult
            }

            if (result.resultCode != Activity.RESULT_OK) {
                currentOnResult.value(GoogleDriveAuthResult.Cancelled)
                return@rememberLauncherForActivityResult
            }

            if (resultIntent == null) {
                currentOnResult.value(
                    GoogleDriveAuthResult.Failure(GoogleDriveAuthException("Google authorization returned no result")),
                )
                return@rememberLauncherForActivityResult
            }

            val response = AuthorizationResponse.fromIntent(resultIntent)
            if (response == null) {
                currentOnResult.value(
                    GoogleDriveAuthResult.Failure(GoogleDriveAuthException("Google authorization returned no response")),
                )
                return@rememberLauncherForActivityResult
            }

            coroutineScope.launch {
                try {
                    val tokenResponse = authorizationService.exchangeCode(response)
                    val refreshToken =
                        tokenResponse.refreshToken
                            ?: throw GoogleDriveAuthException(
                                "Google returned no refresh token. Sign in again and grant offline access.",
                            )
                    currentOnResult.value(
                        GoogleDriveAuthResult.Success(
                            GoogleDriveOAuthCredentials.forRefreshToken(clientId, refreshToken),
                        ),
                    )
                } catch (e: Exception) {
                    Log.e("GoogleDriveAuth", "Unable to exchange Google Drive authorization code", e)
                    currentOnResult.value(GoogleDriveAuthResult.Failure(e))
                }
            }
        }

    return remember(authorizationLauncher, authorizationService, clientId, redirectUri) {
        {
            try {
                val request =
                    AuthorizationRequest
                        .Builder(
                            serviceConfiguration,
                            clientId,
                            ResponseTypeValues.CODE,
                            redirectUri,
                        ).setScopes(DriveScopes.DRIVE)
                        .setPrompt("consent")
                        .setAdditionalParameters(
                            mapOf(
                                "access_type" to "offline",
                            ),
                        ).build()
                authorizationLauncher.launch(authorizationService.getAuthorizationRequestIntent(request))
            } catch (e: Exception) {
                Log.e("GoogleDriveAuth", "Unable to start Google Drive authorization", e)
                currentOnResult.value(GoogleDriveAuthResult.Failure(e))
            }
        }
    }
}

private fun googleDriveRedirectUri(clientId: String): Uri {
    val redirectScheme =
        "com.googleusercontent.apps." +
            clientId.removeSuffix(".apps.googleusercontent.com")
    return Uri.parse("$redirectScheme:/oauth2redirect")
}

private suspend fun AuthorizationService.exchangeCode(response: AuthorizationResponse) =
    suspendCancellableCoroutine { continuation ->
        performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, exception ->
            when {
                tokenResponse != null && continuation.isActive -> continuation.resume(tokenResponse)
                exception != null && continuation.isActive -> continuation.resumeWithException(exception)
                else -> Unit
            }
        }
    }

