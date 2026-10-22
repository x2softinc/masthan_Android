package com.x2softinc.masthan.ui

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.vector.ImageVector
import com.x2softinc.masthan.AppMetadata

/**
 * Legal documents linked from the Settings "Legal" section (port of Views/LegalDocumentView.swift).
 * `.PRIVACY_POLICY` loads its hosted page in a WebView; `.TERMS_OF_USE` is shown natively by
 * [EulaScreen] until a hosted page exists (give it a [hostedUrl] then).
 */
enum class LegalDocument(val title: String, val icon: ImageVector, val hostedUrl: String?) {
    TERMS_OF_USE(AppMetadata.TERMS_OF_USE_TITLE, Icons.Filled.Description, null),
    PRIVACY_POLICY(AppMetadata.PRIVACY_POLICY_TITLE, Icons.Filled.PanTool, AppMetadata.PRIVACY_POLICY_URL),
}

@Composable
fun LegalDocumentScreen(document: LegalDocument) {
    val url = document.hostedUrl
    var isLoading by remember { mutableStateOf(true) }
    var didFail by remember { mutableStateOf(false) }
    var retryToken by remember { mutableIntStateOf(0) }

    if (url == null) {
        Message(Icons.Filled.Description, "Document Unavailable", "The document address for ${document.title} has not been configured.")
        return
    }

    if (didFail) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Icon(Icons.Filled.WifiOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Unable to Load", style = MaterialTheme.typography.titleMedium)
            Text("Check your internet connection and try again.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { didFail = false; isLoading = true; retryToken += 1 }) { Text("Retry") }
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        // A new key recreates the WebView on retry.
        androidx.compose.runtime.key(retryToken) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) { isLoading = true }
                            override fun onPageFinished(view: WebView?, url: String?) { isLoading = false }
                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    didFail = true
                                }
                            }
                        }
                        loadUrl(url)
                    }
                },
            )
        }
        if (isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}

@Composable
private fun Message(icon: ImageVector, title: String, description: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
