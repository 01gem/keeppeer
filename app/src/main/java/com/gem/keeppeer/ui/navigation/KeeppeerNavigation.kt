package com.gem.keeppeer.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.gem.keeppeer.data.repo.EntryRepository
import com.gem.keeppeer.ui.detail.EntryDetailScreen
import com.gem.keeppeer.ui.detail.EntryDetailViewModel
import com.gem.keeppeer.ui.edit.EntryEditScreen
import com.gem.keeppeer.ui.edit.EntryEditViewModel
import com.gem.keeppeer.ui.list.EntryListViewModel
import com.gem.keeppeer.ui.list.PasswordListScreen

private const val LIST = "entries"
private const val ADD = "entry/new"
private const val DETAIL = "entry/{entryId}"
private const val EDIT = "entry/{entryId}/edit"

// Only numeric IDs enter navigation arguments. ViewModels own all entry data.
@Composable
fun KeeppeerApp(repository: EntryRepository, onSettings: () -> Unit, onCopy: (String) -> Boolean) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = LIST) {
        composable(LIST) {
            val vm: EntryListViewModel = viewModel(factory = viewModelFactory {
                initializer { EntryListViewModel(repository) }
            })
            val state by vm.state.collectAsStateWithLifecycle()
            PasswordListScreen(state, vm::search, vm::retry,
                onAdd = { nav.navigate(ADD) },
                onEntry = { nav.navigate("entry/$it") },
                onSettings = onSettings,
                onCategory = vm::category, onFavoritesOnly = vm::favoritesOnly,
                onClear = vm::clearFilters, onFavorite = vm::favorite,
            )
        }
        composable(ADD) {
            val vm: EntryEditViewModel = viewModel(factory = viewModelFactory {
                initializer { EntryEditViewModel(repository, null) }
            })
            val state by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.savedId) {
                state.savedId?.let { id ->
                    nav.navigate("entry/$id") { popUpTo(LIST) }
                }
            }
            EntryEditScreen(state, false, vm::change, vm::save, { nav.popBackStack() }, vm::load, vm::openGenerator, onSettings)
            if (state.generatorOpen) com.gem.keeppeer.ui.edit.GeneratorDialog(state, vm::generatorOptions,
                vm::regenerate, vm::useGenerated, vm::dismissGenerator, onCopy)
        }
        composable(DETAIL, arguments = listOf(navArgument("entryId") { type = NavType.LongType })) { backStack ->
            val id = requireNotNull(backStack.arguments).getLong("entryId")
            val vm: EntryDetailViewModel = viewModel(factory = viewModelFactory {
                initializer { EntryDetailViewModel(repository, id) }
            })
            val state by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.deleted) {
                if (state.deleted) nav.popBackStack(LIST, inclusive = false)
            }
            EntryDetailScreen(state, { nav.popBackStack() }, { nav.navigate("entry/$id/edit") }, vm::delete, vm::retry, onCopy)
        }
        composable(EDIT, arguments = listOf(navArgument("entryId") { type = NavType.LongType })) { backStack ->
            val id = requireNotNull(backStack.arguments).getLong("entryId")
            val vm: EntryEditViewModel = viewModel(factory = viewModelFactory {
                initializer { EntryEditViewModel(repository, id) }
            })
            val state by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.savedId) {
                if (state.savedId != null) nav.popBackStack()
            }
            EntryEditScreen(state, true, vm::change, vm::save, { nav.popBackStack() }, vm::load, vm::openGenerator, onSettings)
            if (state.generatorOpen) com.gem.keeppeer.ui.edit.GeneratorDialog(state, vm::generatorOptions,
                vm::regenerate, vm::useGenerated, vm::dismissGenerator, onCopy)
        }
    }
}
