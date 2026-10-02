package com.cute.wallpaper.ringtones.presentation.search

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : BaseFragment<FragmentSearchBinding>() {
    private val viewModel: SearchViewModel by viewModels()
    private var tabMediator: TabLayoutMediator? = null

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
    }

    private fun createCategoryTab(page: SearchPageUiModel): TextView {
        return TextView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36))
            minWidth = dp(72)
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
            setTextAppearance(R.style.Body14R)
            text = "${page.emoji} ${page.label}"
            isClickable = false
            isFocusable = false
            updateCategoryTab(this, false)
        }
    }

    private fun createColorTab(page: SearchPageUiModel): FrameLayout {
        val fillColor = runCatching { Color.parseColor(page.colorHex) }
            .getOrDefault(ContextCompat.getColor(requireContext(), R.color.primary_100))
        return FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(dp(44), dp(44))
            isClickable = false
            isFocusable = false

            addView(View(context).apply {
                background = colorCircleDrawable(page, fillColor, false)
            }, FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER))

            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_search_color_check)
                setColorFilter(
                    ContextCompat.getColor(
                        context,
                        if (page.id == "white") R.color.primary_500 else R.color.neutral_0
                    )
                )
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                visibility = View.GONE
            }, FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER))
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
        view.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selected) R.color.primary_500 else R.color.neutral_900
            )
        )
        view.typeface = ResourcesCompat.getFont(
            requireContext(),
            if (selected) R.font.poppins_medium else R.font.poppins_regular
        )
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
            setStroke(
                dp(1),
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.primary_500 else R.color.primary_200
                )
            )
        }
    }

    private fun updateColorTab(container: FrameLayout, page: SearchPageUiModel, selected: Boolean) {
        val fillColor = runCatching { Color.parseColor(page.colorHex) }
            .getOrDefault(ContextCompat.getColor(requireContext(), R.color.primary_100))
        container.getChildAt(0).background = colorCircleDrawable(page, fillColor, selected)
        container.getChildAt(1).isVisible = selected
    }

    private fun colorCircleDrawable(
        page: SearchPageUiModel,
        fillColor: Int,
        selected: Boolean
    ) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fillColor)
        val strokeColor = when {
            selected -> ContextCompat.getColor(requireContext(), R.color.neutral_0)
            page.id == "white" -> ContextCompat.getColor(requireContext(), R.color.neutral_200)
            else -> Color.TRANSPARENT
        }
        setStroke(if (selected || page.id == "white") dp(2) else 0, strokeColor)
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
