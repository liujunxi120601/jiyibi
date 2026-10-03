package com.jiyibi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jiyibi.app.ui.TransactionViewModel
import com.jiyibi.app.ui.add.AddTransactionScreen
import com.jiyibi.app.ui.budget.BudgetScreen
import com.jiyibi.app.ui.categories.CategoriesScreen
import com.jiyibi.app.ui.goals.GoalsScreen
import com.jiyibi.app.ui.home.HomeScreen
import com.jiyibi.app.ui.lock.LockScreen
import com.jiyibi.app.ui.poster.PosterScreen
import com.jiyibi.app.ui.recurring.RecurringScreen
import com.jiyibi.app.ui.search.SearchScreen
import com.jiyibi.app.ui.settings.SettingsScreen
import com.jiyibi.app.ui.stats.StatsScreen
import com.jiyibi.app.ui.theme.JiyibiTheme
import com.jiyibi.app.util.AppLock

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JiyibiTheme {
                val vm: TransactionViewModel = viewModel()
                // 启动时生成已到期的周期性账目
                LaunchedEffect(Unit) { vm.generateRecurring() }
                AppGate(vm)
            }
        }
    }
}

/** 应用锁门禁：开启了应用锁且尚未解锁时，先显示锁屏 */
@Composable
private fun AppGate(vm: TransactionViewModel) {
    val context = LocalContext.current
    var unlocked by remember { mutableStateOf(!AppLock.isEnabled(context)) }
    var lockMode by remember { mutableStateOf("verify") } // verify / setup / change

    if (!unlocked) {
        LockScreen(
            mode = lockMode,
            onUnlock = { unlocked = true; lockMode = "verify" },
            onCancel = if (lockMode != "verify") {
                {
                    // 设置新 PIN 时取消：直接回 App；修改 PIN 时取消：回到验证页
                    if (AppLock.isEnabled(context)) lockMode = "verify"
                    else { unlocked = true; lockMode = "verify" }
                }
            } else null
        )
    } else {
        JiyibiNavHost(
            vm = vm,
            onLockSetup = { unlocked = false; lockMode = "setup" },
            onLockChange = { unlocked = false; lockMode = "change" }
        )
    }
}

@Composable
fun JiyibiNavHost(
    vm: TransactionViewModel,
    onLockSetup: () -> Unit,
    onLockChange: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomBarRoutes = setOf("home", "stats", "settings")
    val showBottomBar = currentRoute in bottomBarRoutes

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo("home") { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = { navigateTab("home") },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "首页") },
                        label = { Text("首页") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "stats",
                        onClick = { navigateTab("stats") },
                        icon = { Icon(Icons.Filled.PieChart, contentDescription = "统计") },
                        label = { Text("统计") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = { navigateTab("settings") },
                        icon = { Icon(Icons.Filled.Person, contentDescription = "我的") },
                        label = { Text("我的") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (showBottomBar) {
                FloatingActionButton(onClick = { navController.navigate("add") }) {
                    Icon(Icons.Filled.Add, contentDescription = "记一笔")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen(vm, onSearch = { navController.navigate("search") }) }
            composable("stats") { StatsScreen(vm) }
            composable("settings") {
                SettingsScreen(
                    vm = vm,
                    onBack = { navigateTab("home") },
                    onNavigate = { navController.navigate(it) },
                    onLockSetup = onLockSetup,
                    onLockChange = onLockChange
                )
            }
            composable("add") {
                AddTransactionScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("budgets") {
                BudgetScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("search") {
                SearchScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("goals") {
                GoalsScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("recurring") {
                RecurringScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("categories") {
                CategoriesScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("poster") {
                PosterScreen(vm = vm, onBack = { navController.popBackStack() })
            }
        }
    }
}
