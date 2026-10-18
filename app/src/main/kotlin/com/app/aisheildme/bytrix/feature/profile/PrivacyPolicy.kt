package com.app.aisheildme.bytrix.feature.profile

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.ErrorState
import com.app.aisheildme.bytrix.core.designsystem.component.FullScreenLoader
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.data.repository.ContentRepository
import com.app.aisheildme.bytrix.navigation.StaticPageRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StaticPageUiState(
    val title: String = "Privacy Policy",
    val html: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class StaticPageViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: StaticPageRoute = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(StaticPageUiState())
    val uiState: StateFlow<StaticPageUiState> = _uiState.asStateFlow()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val page = contentRepository.getPage(args.slug)
                _uiState.update {
                    it.copy(
                        title = page.title.ifEmpty { it.title },
                        html = page.content,
                        isLoading = false,
                    )
                }
            } catch (e: AppException) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}

@Composable
fun StaticPageScreen(
    onBack: () -> Unit,
    viewModel: StaticPageViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SimpleAppBar(title = state.title, onBack = onBack) },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> FullScreenLoader()
                state.error != null -> ErrorState(state.error!!, viewModel::fetch)
                else -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = false
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            null,
                            wrapHtml(state.html),
                            "text/html",
                            "utf-8",
                            null,
                        )
                    },
                )
            }
        }
    }
}

private fun wrapHtml(body: String): String = """
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          body {
            font-family: -apple-system, Roboto, sans-serif;
            font-size: 15px;
            line-height: 1.6;
            color: #101010;
            background: transparent;
            padding: 16px;
            margin: 0;
          }
          h1, h2, h3 { color: #0A0A0A; }
          a { color: #1A1AE5; }
          img { max-width: 100%; height: auto; }
        </style>
      </head>
      <body>$body</body>
    </html>
""".trimIndent()
