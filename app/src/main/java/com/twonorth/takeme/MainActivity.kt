package com.twonorth.takeme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.twonorth.takeme.data.Medication
import com.twonorth.takeme.ui.insights.InsightsScreen
import com.twonorth.takeme.ui.onboarding.OnboardingScreen
import com.twonorth.takeme.ui.theme.TakeMeTheme
import com.twonorth.takeme.ui.today.MedicationStatus
import com.twonorth.takeme.ui.today.TodayViewModel
import kotlinx.coroutines.flow.first

sealed class Screen(val route: String, val resourceId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Today : Screen("today", R.string.nav_today, Icons.Default.DateRange)
    object Insights : Screen("insights", R.string.nav_insights, Icons.Default.Info)
    object Onboarding : Screen("onboarding", 0, Icons.Default.Add)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val app = application as TakeMeApplication
        
        setContent {
            TakeMeTheme {
                var startDestination by remember { mutableStateOf<String?>(null) }
                
                LaunchedEffect(Unit) {
                    val onboardingComplete = app.userPreferences.onboardingComplete.first()
                    val meds = app.repository.allMedications.first()
                    
                    startDestination = if (!onboardingComplete && meds.isEmpty()) {
                        Screen.Onboarding.route
                    } else {
                        Screen.Today.route
                    }
                }
                
                startDestination?.let { dest ->
                    MainNavigation(dest)
                }
            }
        }
    }
}

@Composable
fun MainNavigation(startDestination: String) {
    val navController = rememberNavController()
    val items = listOf(Screen.Today, Screen.Insights)
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != Screen.Onboarding.route
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(stringResource(screen.resourceId)) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = startDestination, Modifier.padding(innerPadding)) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Today.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Today.route) { TodayScreenContainer() }
            composable(Screen.Insights.route) { InsightsScreen() }
        }
    }
}

@Composable
fun TodayScreenContainer(viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var medicationToDelete by remember { mutableStateOf<Medication?>(null) }
    var medicationToSkip by remember { mutableStateOf<MedicationStatus?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val skipRemovedMsg = stringResource(R.string.snackbar_skip_removed)
    val undoLabel = stringResource(R.string.action_undo)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
            }
        }
    ) { innerPadding ->
        TodayScreen(
            statusList = uiState.medications,
            onToggleTaken = { viewModel.toggleTaken(it) },
            onSkipRequest = { medicationToSkip = it },
            onUnskipRequest = { status ->
                viewModel.unskipDose(status)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = skipRemovedMsg,
                        actionLabel = undoLabel
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreLastSkip()
                    }
                }
            },
            onDeleteRequest = { medicationToDelete = it },
            modifier = Modifier.padding(innerPadding)
        )

        if (showAddDialog) {
            AddMedicationDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name ->
                    viewModel.addMedication(name)
                    showAddDialog = false
                }
            )
        }

        medicationToDelete?.let { med ->
            DeleteMedicationDialog(
                medicationName = med.name,
                onDismiss = { medicationToDelete = null },
                onConfirm = {
                    viewModel.deleteMedication(med)
                    medicationToDelete = null
                }
            )
        }

        medicationToSkip?.let { status ->
            SkipReasonDialog(
                onDismiss = { medicationToSkip = null },
                onConfirm = { reason, note ->
                    viewModel.skipDose(status, reason, note)
                    medicationToSkip = null
                }
            )
        }
    }
}

@Composable
fun TodayScreen(
    statusList: List<MedicationStatus>,
    onToggleTaken: (MedicationStatus) -> Unit,
    onSkipRequest: (MedicationStatus) -> Unit,
    onUnskipRequest: (MedicationStatus) -> Unit,
    onDeleteRequest: (Medication) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.today_heading),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (statusList.isNotEmpty()) {
            val takenCount = statusList.count { it.isTakenToday }
            Text(
                text = stringResource(R.string.today_progress, takenCount, statusList.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn {
                items(statusList, key = { it.medication.id }) { status ->
                    MedicationItem(
                        status = status,
                        onToggleTaken = onToggleTaken,
                        onSkipRequest = { onSkipRequest(status) },
                        onUnskipRequest = { onUnskipRequest(status) },
                        onLongClick = { onDeleteRequest(status.medication) }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.empty_today),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MedicationItem(
    status: MedicationStatus,
    onToggleTaken: (MedicationStatus) -> Unit,
    onSkipRequest: () -> Unit,
    onUnskipRequest: () -> Unit,
    onLongClick: () -> Unit
) {
    val isTaken = status.isTakenToday
    val isSkipped = status.isSkippedToday
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(
                onClick = { /* Tapping card body does nothing now */ },
                onLongClick = onLongClick
            ),
        colors = if (isTaken || isSkipped) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(getMedicationColor(status.medication.color))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = status.medication.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSkipped) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
                )
                if (isSkipped) {
                    val reasonLabel = when(status.skipRecord?.reason) {
                        "forgot" -> stringResource(R.string.reason_forgot)
                        "side_effects" -> stringResource(R.string.reason_side_effects)
                        "ran_out" -> stringResource(R.string.reason_ran_out)
                        "felt_better" -> stringResource(R.string.reason_felt_better)
                        else -> status.skipRecord?.note ?: stringResource(R.string.reason_other)
                    }
                    Text(
                        text = stringResource(R.string.skipped_format, reasonLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isSkipped) {
                if (!isTaken) {
                    TextButton(onClick = onSkipRequest) {
                        Text(stringResource(R.string.action_skip))
                    }
                }
                
                IconButton(
                    onClick = { onToggleTaken(status) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = if (isTaken) stringResource(R.string.action_taken) else stringResource(R.string.action_mark_taken),
                        tint = if (isTaken) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun getMedicationColor(colorLabel: String): Color {
    return when (colorLabel.lowercase()) {
        "amber" -> MaterialTheme.colorScheme.primary
        "blue" -> Color(0xFF4A90E2)
        "green" -> Color(0xFF7ED321)
        "red" -> Color(0xFFD0021B)
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun AddMedicationDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.dialog_add_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun SkipReasonDialog(onDismiss: () -> Unit, onConfirm: (String, String?) -> Unit) {
    var selectedReason by remember { mutableStateOf("forgot") }
    var note by remember { mutableStateOf("") }
    
    val reasons = listOf(
        "forgot" to R.string.reason_forgot,
        "side_effects" to R.string.reason_side_effects,
        "ran_out" to R.string.reason_ran_out,
        "felt_better" to R.string.reason_felt_better,
        "other" to R.string.reason_other
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_skip_title)) },
        text = {
            Column {
                reasons.forEach { (value, labelRes) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = { selectedReason = value })
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (selectedReason == value),
                            onClick = { selectedReason = value }
                        )
                        Text(
                            text = stringResource(labelRes),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                if (selectedReason == "other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.dialog_skip_note_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedReason, if (selectedReason == "other") note else null) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun DeleteMedicationDialog(
    medicationName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_title)) },
        text = {
            Text(stringResource(R.string.dialog_delete_message, medicationName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
