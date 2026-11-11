package com.cosmos.unreddit

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.cosmos.unreddit.data.model.preferences.BrandingPreferences
import com.cosmos.unreddit.databinding.ActivityMainBinding
import com.cosmos.unreddit.ui.main.StealthNavigationBar
import com.cosmos.unreddit.ui.main.TopLevelDestination
import com.cosmos.unreddit.ui.policydisclaimer.PolicyDisclaimerDialogFragment
import com.cosmos.unreddit.ui.postlist.PostListFragment
import com.cosmos.unreddit.ui.theme.StealthTheme
import com.cosmos.unreddit.util.extension.currentNavigationFragment
import com.cosmos.unreddit.util.extension.isPast
import com.cosmos.unreddit.util.extension.launchRepeat
import com.cosmos.unreddit.util.extension.unredditApplication
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: UiViewModel by viewModels()
    private lateinit var navController: NavController
    private var policyDisclaimerSnackbar: Snackbar? = null

    private var selectedDestination by mutableStateOf<TopLevelDestination?>(TopLevelDestination.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(unredditApplication.appTheme)
        if (unredditApplication.colorScheme == BrandingPreferences.ColorScheme.DYNAMIC) {
            // Legacy View screens follow the wallpaper colors too.
            DynamicColors.applyToActivityIfAvailable(this)
        }
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideLegacyDrawerButtons()
        initNavigation()
        initNavigationBar()

        launchRepeat(Lifecycle.State.STARTED) {
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
        navController.addOnDestinationChangedListener(this)
    }

    private fun initNavigationBar() {
        binding.navigationBar.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val visible by viewModel.navigationVisibility.collectAsState()
                StealthTheme(
                    scheme = unredditApplication.colorScheme,
                    amoled = unredditApplication.isAmoled,
                ) {
                    StealthNavigationBar(
                        visible = visible,
                        selected = selectedDestination,
                        onSelect = ::navigateTo,
                        onReselect = { destination ->
                            if (destination == TopLevelDestination.HOME) {
                                (currentNavigationFragment as? PostListFragment)?.scrollToTop()
                            }
                        },
                    )
                }
            }
        }
    }

    private fun navigateTo(destination: TopLevelDestination) {
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(navController.graph.findStartDestination().id, false, true)
            .build()
        navController.navigate(destination.graphId, null, options)
    }

    /**
     * The side drawer is gone; the app bars of not-yet-rebuilt screens still carry its
     * menu button, so hide it as each view is created.
     */
    private fun hideLegacyDrawerButtons() {
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?
                ) {
                    v.findViewById<View>(R.id.menu_card)?.visibility = View.GONE
                }
            },
            true
        )
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
        }.setActionTextColor(ContextCompat.getColor(this, R.color.white)).apply {
            if (viewModel.navigationVisibility.value) anchorView = binding.navigationBar
            show()
        }
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        val topLevel = destination.id in TOP_LEVEL_SCREENS
        viewModel.setNavigationVisibility(topLevel)
        selectedDestination = TopLevelDestination.entries.firstOrNull { item ->
            destination.hierarchy.any { it.id == item.graphId }
        }
        if (topLevel) {
            // Bring the bar back if it was scrolled away on the previous screen.
            val params = binding.navigationBar.layoutParams as CoordinatorLayout.LayoutParams
            (params.behavior as? HideBottomViewOnScrollBehavior<View>)
                ?.slideUp(binding.navigationBar)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        policyDisclaimerSnackbar = null
    }

    companion object {
        private val TOP_LEVEL_SCREENS = setOf(
            R.id.postListFragment, R.id.subscriptionsFragment,
            R.id.profileFragment, R.id.preferencesFragment
        )
        private val POLICY_DISCLAIMER_DATE =
            Calendar.getInstance().apply { set(2023, 5, 10) }.timeInMillis
        private const val POLICY_DISCLAIMER_DELAY: Long = 5000
    }
}
