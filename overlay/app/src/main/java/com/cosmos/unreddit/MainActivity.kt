package com.cosmos.unreddit

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.cosmos.unreddit.MainActivity.BottomNavigationState.LEFT_HANDED
import com.cosmos.unreddit.MainActivity.BottomNavigationState.NOT_INITIALIZED
import com.cosmos.unreddit.MainActivity.BottomNavigationState.RIGHT_HANDED
import com.cosmos.unreddit.data.model.preferences.BrandingPreferences
import com.cosmos.unreddit.databinding.ActivityMainBinding
import com.cosmos.unreddit.ui.policydisclaimer.PolicyDisclaimerDialogFragment
import com.cosmos.unreddit.ui.postlist.PostListFragment
import com.cosmos.unreddit.util.HideBottomViewBehavior
import com.cosmos.unreddit.util.extension.clearWindowInsetsListener
import com.cosmos.unreddit.util.extension.currentNavigationFragment
import com.cosmos.unreddit.util.extension.isPast
import com.cosmos.unreddit.util.extension.launchRepeat
import com.cosmos.unreddit.util.extension.unredditApplication
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: UiViewModel by viewModels()
    private lateinit var navController: NavController
    private var bottomNavigationState = NOT_INITIALIZED
    private var policyDisclaimerSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(unredditApplication.appTheme)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initNavigation()
        applyNavigationLayout(viewModel.navigationLayout.value)

        launchRepeat(Lifecycle.State.STARTED) {
            launch {
                viewModel.navigationVisibility.drop(1).collect(this@MainActivity::showNavigation)
            }
            launch {
                viewModel.leftHandedMode.collect { leftHandedMode ->
                    when (bottomNavigationState) {
                        NOT_INITIALIZED -> initBottomNavigationView(leftHandedMode)
                        RIGHT_HANDED -> if (leftHandedMode) initBottomNavigationView(true)
                        LEFT_HANDED -> if (!leftHandedMode) initBottomNavigationView(false)
                    }
                }
            }
            launch { viewModel.navigationLayout.collect(::applyNavigationLayout) }
            launch {
                viewModel.policyDisclaimerShown.collect { shown ->
                    if (!shown && POLICY_DISCLAIMER_DATE.isPast) {
                        delay(POLICY_DISCLAIMER_DELAY)
                        showPolicyDisclaimerSnackbar()
                    }
                }
            }
        }
    }

    private fun initNavigation() {
        val host = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        // Assign before registering: Navigation dispatches the current destination immediately.
        navController = host.navController
        navController.addOnDestinationChangedListener(this@MainActivity)

        binding.bottomNavigation.run {
            setupWithNavController(navController)
            setOnItemReselectedListener {
                if (it.itemId == R.id.home) {
                    (currentNavigationFragment as? PostListFragment)?.scrollToTop()
                }
            }
        }

        binding.appNavigation.setNavigationItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.appearance -> { showColorSchemeDialog(); true }
                R.id.navigation_layout -> { showNavigationLayoutDialog(); true }
                else -> NavigationUI.onNavDestinationSelected(item, navController)
            }
            if (handled) binding.appDrawer.closeDrawer(GravityCompat.START)
            handled
        }
    }

    /** Invoked by the top-level app bar menu buttons. */
    fun openAppDrawer(view: View) {
        if (!binding.appDrawer.isDrawerOpen(GravityCompat.START)) {
            binding.appDrawer.openDrawer(GravityCompat.START)
        }
    }

    private fun applyNavigationLayout(layout: BrandingPreferences.NavigationLayout) {
        val sideMenuEnabled = layout == BrandingPreferences.NavigationLayout.SIDE_MENU
        binding.bottomNavigation.visibility =
            if (sideMenuEnabled) View.GONE else View.VISIBLE
        if (!sideMenuEnabled) {
            showNavigation(viewModel.navigationVisibility.value, false)
        }
        binding.appDrawer.setDrawerLockMode(
            if (sideMenuEnabled) androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED
            else androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
            GravityCompat.START
        )
        currentNavigationFragment?.view
            ?.findViewById<View>(R.id.menu_card)
            ?.visibility = if (sideMenuEnabled) View.VISIBLE else View.GONE
        updateCheckedDestination(navController.currentDestination)
    }

    private fun updateCheckedDestination(destination: NavDestination?) {
        val item = when (destination?.id) {
            R.id.subscriptionsFragment -> R.id.subscriptions
            R.id.profileFragment -> R.id.profile
            R.id.preferencesFragment -> R.id.settings
            else -> R.id.home
        }
        binding.appNavigation.setCheckedItem(item)
    }

    private fun showColorSchemeDialog() {
        val schemes = BrandingPreferences.ColorScheme.values()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.brand_color_scheme_title)
            .setSingleChoiceItems(
                R.array.brand_color_scheme_labels,
                unredditApplication.colorScheme.ordinal
            ) { dialog, index ->
                val scheme = schemes.getOrNull(index) ?: return@setSingleChoiceItems
                viewModel.setColorScheme(scheme)
                unredditApplication.applyColorScheme(scheme)
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showNavigationLayoutDialog() {
        val layouts = BrandingPreferences.NavigationLayout.values()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.brand_navigation_layout_title)
            .setSingleChoiceItems(
                R.array.brand_navigation_layout_labels,
                viewModel.navigationLayout.value.ordinal
            ) { dialog, index ->
                layouts.getOrNull(index)?.let(viewModel::setNavigationLayout)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun initBottomNavigationView(leftHandedMode: Boolean) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom +
                    resources.getDimension(R.dimen.bottom_navigation_margin).toInt()
            }
            view.clearWindowInsetsListener()
            windowInsets
        }

        binding.bottomNavigation.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            gravity = if (leftHandedMode) Gravity.BOTTOM or Gravity.START
            else Gravity.BOTTOM or Gravity.END
            behavior = HideBottomViewBehavior<BottomNavigationView>(leftHandedMode)
        }

        val radius = resources.getDimension(R.dimen.bottom_navigation_radius)
        (binding.bottomNavigation.background as? MaterialShapeDrawable)?.let { background ->
            val builder = background.shapeAppearanceModel.toBuilder()
            if (leftHandedMode) {
                builder.setTopRightCorner(CornerFamily.ROUNDED, radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED, radius)
                    .setTopLeftCorner(CornerFamily.CUT, 0F)
                    .setBottomLeftCorner(CornerFamily.CUT, 0F)
            } else {
                builder.setTopRightCorner(CornerFamily.CUT, 0F)
                    .setBottomRightCorner(CornerFamily.CUT, 0F)
                    .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, radius)
            }
            background.shapeAppearanceModel = builder.build()
        }

        binding.bottomNavigation.post { showNavigation(viewModel.navigationVisibility.value, false) }
        bottomNavigationState = if (leftHandedMode) LEFT_HANDED else RIGHT_HANDED
    }

    private fun showNavigation(show: Boolean, animate: Boolean = true) {
        if (viewModel.navigationLayout.value != BrandingPreferences.NavigationLayout.CLASSIC) return
        val params = binding.bottomNavigation.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as HideBottomViewBehavior?
        if (show) behavior?.run {
            enabled = true
            slideIn(binding.bottomNavigation, animate)
        } else behavior?.run {
            enabled = false
            slideOut(binding.bottomNavigation, animate)
        }
    }

    private fun showPolicyDisclaimerSnackbar() {
        policyDisclaimerSnackbar = Snackbar.make(
            binding.root,
            getString(
                R.string.snackbar_policy_disclaimer_message,
                getString(R.string.brand_app_name)
            ),
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.snackbar_policy_disclaimer_action) {
            PolicyDisclaimerDialogFragment.show(supportFragmentManager)
            policyDisclaimerSnackbar = null
        }.setActionTextColor(ContextCompat.getColor(this, R.color.white)).apply { show() }
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        if (destination.id in setOf(
                R.id.postListFragment, R.id.subscriptionsFragment,
                R.id.profileFragment, R.id.preferencesFragment
            )
        ) viewModel.setNavigationVisibility(true)
        else viewModel.setNavigationVisibility(false)

        applyNavigationLayout(viewModel.navigationLayout.value)
    }

    override fun onDestroy() {
        super.onDestroy()
        policyDisclaimerSnackbar = null
        bottomNavigationState = NOT_INITIALIZED
    }

    private enum class BottomNavigationState { NOT_INITIALIZED, RIGHT_HANDED, LEFT_HANDED }

    companion object {
        private val POLICY_DISCLAIMER_DATE =
            Calendar.getInstance().apply { set(2023, 5, 10) }.timeInMillis
        private const val POLICY_DISCLAIMER_DELAY: Long = 5000
    }
}
