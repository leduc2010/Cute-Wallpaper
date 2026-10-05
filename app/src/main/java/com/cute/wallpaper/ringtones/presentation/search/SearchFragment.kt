package com.cute.wallpaper.ringtones.presentation.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentSearchBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneActionEvent
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : BaseFragment<FragmentSearchBinding>() {
    private val viewModel: SearchViewModel by viewModels()
    private val ringtoneViewModel: RingtoneViewModel by viewModels()
    private var tabMediator: TabLayoutMediator? = null

    private val writeSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        ringtoneViewModel.retryPendingSet()
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ringtoneViewModel.retryPendingSet()
        } else {
            showToast(R.string.ringtone_storage_permission_required)
        }
    }

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            viewModel.selectPage(position)
        }
    }

    private val tabSelectionListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) = renderTabSelection(tab, true)

        override fun onTabUnselected(tab: TabLayout.Tab) = renderTabSelection(tab, false)

        override fun onTabReselected(tab: TabLayout.Tab) = Unit
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSearchBinding {
        return FragmentSearchBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        WindowCompat.getInsetsController(requireActivity().window, binding.root)
            .isAppearanceLightStatusBars = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val safeArea = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(top = safeArea.top, left = safeArea.left, right = safeArea.right)
            insets
        }

        binding.etSearch.apply {
            isVisible = viewModel.mode != SearchMode.COLOR
            setHint(viewModel.tab.searchHintRes)
            setText(viewModel.query.value.orEmpty())
            setSelection(text?.length ?: 0)
        }

        binding.resultPager.apply {
            adapter = SearchFilterPagerAdapter(
                childFragmentManager,
                viewLifecycleOwner.lifecycle,
                viewModel.pages
            )
            isSaveEnabled = false
            isUserInputEnabled = true
            offscreenPageLimit = 1
            val initialPage = viewModel.selectedPageIndex.value
                ?.takeIf { it in viewModel.pages.indices }
                ?: viewModel.initialPageIndex
            setCurrentItem(initialPage, false)
            registerOnPageChangeCallback(pageCallback)
        }

        binding.filterTabs.addOnTabSelectedListener(tabSelectionListener)
        tabMediator = TabLayoutMediator(binding.filterTabs, binding.resultPager) { tab, position ->
            val page = viewModel.pages[position]
            tab.customView = if (viewModel.mode == SearchMode.COLOR) {
                createColorTab(page)
            } else {
                createCategoryTab(page)
            }
            tab.contentDescription = getString(R.string.search_filter_description, page.label)
        }.also { it.attach() }

        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener { navViewModel.back() }
        binding.etSearch.doAfterTextChanged { viewModel.updateQuery(it?.toString().orEmpty()) }
        binding.etSearch.setOnEditorActionListener { view, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
            val keyboard = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            keyboard.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
            true
        }
    }

    override fun observeData() {
        viewModel.query.observe(viewLifecycleOwner) { query ->
            if (binding.etSearch.text?.toString() != query) {
                binding.etSearch.setText(query)
                binding.etSearch.setSelection(query.length)
            }
        }
        if (
            viewModel.tab == com.cute.wallpaper.ringtones.presentation.main.MainTab.RINGTONES ||
            viewModel.tab == com.cute.wallpaper.ringtones.presentation.main.MainTab.FAVORITES
        ) {
            ringtoneViewModel.actionEvent.observe(viewLifecycleOwner) { event ->
                event.getContentIfNotHandled()?.let(::handleRingtoneAction)
            }
        }
    }

    private fun handleRingtoneAction(event: RingtoneActionEvent) {
        when (event) {
            RingtoneActionEvent.RequestWriteSettings -> {
                showToast(R.string.ringtone_write_settings_required)
                writeSettingsLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${requireContext().packageName}")
                    }
                )
            }

            RingtoneActionEvent.RequestStoragePermission -> {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            is RingtoneActionEvent.SetSuccess -> showToast(R.string.ringtone_set_success)
            RingtoneActionEvent.SetFailed -> showToast(R.string.ringtone_set_failed)
            RingtoneActionEvent.PreviewFailed -> showToast(R.string.ringtone_preview_failed)
        }
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun createCategoryTab(page: SearchPageUiModel): TextView {
        return (layoutInflater.inflate(
            R.layout.item_search_category_tab,
            binding.filterTabs,
            false
        ) as TextView).apply {
            text = listOf(page.emoji, page.label)
                .filter(String::isNotBlank)
                .joinToString(" ")
            updateCategoryTab(this, false)
        }
    }

    private fun createColorTab(page: SearchPageUiModel): FrameLayout {
        val fillColor = runCatching { Color.parseColor(page.colorHex) }
            .getOrDefault(ContextCompat.getColor(requireContext(), R.color.primary_100))
        return (layoutInflater.inflate(
            R.layout.item_search_color_tab,
            binding.filterTabs,
            false
        ) as FrameLayout).apply {
            updateColorSwatch(
                findViewById(R.id.colorSwatch),
                page,
                fillColor,
                false
            )
            findViewById<ImageView>(R.id.selectedIcon).apply {
                setColorFilter(
                    ContextCompat.getColor(
                        context,
                        if (page.id == "white") R.color.primary_500 else R.color.neutral_0
                    )
                )
            }
        }
    }

    private fun renderTabSelection(tab: TabLayout.Tab, selected: Boolean) {
        val page = viewModel.pages.getOrNull(tab.position) ?: return
        when (val customView = tab.customView) {
            is TextView -> updateCategoryTab(customView, selected)
            is FrameLayout -> updateColorTab(customView, page, selected)
        }
    }

    private fun updateCategoryTab(view: TextView, selected: Boolean) {
        view.isSelected = selected
        view.typeface = ResourcesCompat.getFont(
            requireContext(),
            if (selected) R.font.poppins_medium else R.font.poppins_regular
        )
    }

    private fun updateColorTab(container: FrameLayout, page: SearchPageUiModel, selected: Boolean) {
        val fillColor = runCatching { Color.parseColor(page.colorHex) }
            .getOrDefault(ContextCompat.getColor(requireContext(), R.color.primary_100))
        container.isSelected = selected
        updateColorSwatch(
            container.findViewById(R.id.colorSwatch),
            page,
            fillColor,
            selected
        )
        container.findViewById<ImageView>(R.id.selectedIcon).isVisible = selected
    }

    private fun updateColorSwatch(
        swatch: MaterialCardView,
        page: SearchPageUiModel,
        fillColor: Int,
        selected: Boolean
    ) {
        swatch.setCardBackgroundColor(fillColor)
        val strokeColor = when {
            selected -> ContextCompat.getColor(requireContext(), R.color.neutral_0)
            page.id == "white" -> ContextCompat.getColor(requireContext(), R.color.neutral_200)
            else -> Color.TRANSPARENT
        }
        swatch.strokeColor = strokeColor
    }

    override fun onPause() {
        if (
            viewModel.tab == com.cute.wallpaper.ringtones.presentation.main.MainTab.RINGTONES ||
            viewModel.tab == com.cute.wallpaper.ringtones.presentation.main.MainTab.FAVORITES
        ) {
            ringtoneViewModel.stopPreview()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        binding.filterTabs.removeOnTabSelectedListener(tabSelectionListener)
        tabMediator?.detach()
        tabMediator = null
        binding.resultPager.unregisterOnPageChangeCallback(pageCallback)
        binding.resultPager.adapter = null
        super.onDestroyView()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
