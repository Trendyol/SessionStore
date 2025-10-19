package com.trendyol.sessionstore.sample.datadetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trendyol.sessionstore.sample.common.ui.theme.SessionStoreTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DataDetailFragment : Fragment() {

    private val viewModel: DataDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                SessionStoreTheme {
                    DataDetailScreen(
                        onNavigateBack = {
                            parentFragmentManager.popBackStack()
                        },
                        uiState = uiState,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY_SESSION_DATA = "key_session_data"

        fun newInstance(key: String): DataDetailFragment = DataDetailFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_SESSION_DATA, key)
            }
        }
    }
}
