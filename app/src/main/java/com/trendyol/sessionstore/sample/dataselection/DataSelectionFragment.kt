package com.trendyol.sessionstore.sample.dataselection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.trendyol.sessionstore.sample.common.ui.theme.SessionStoreTheme
import com.trendyol.sessionstore.sample.datadetail.DataDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DataSelectionFragment : Fragment() {

    private val viewModel: SessionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SessionStoreTheme {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    DataSelectionScreen(
                        selectedSize = uiState.selectedSize,
                        isLoading = uiState.isLoading,
                        onSizeSelected = viewModel::updateSelectedSize,
                        onStoreDataAndNavigate = viewModel::storeDataAndNavigate
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.navigateToDataDetail.collect { navigateToDetail(it) }
                }
            }
        }
    }

    private fun navigateToDetail(key: String) {
        val detailFragment = DataDetailFragment.newInstance(key)
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, detailFragment)
            .addToBackStack(null)
            .commit()
    }
}
