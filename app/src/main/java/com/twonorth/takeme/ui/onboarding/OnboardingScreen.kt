package com.twonorth.takeme.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twonorth.takeme.R

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onFinish()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState.currentStep) {
                OnboardingStep.NAME -> NameStep(
                    name = uiState.medicationName,
                    onNameChange = viewModel::updateName,
                    onNext = viewModel::submitName
                )
                OnboardingStep.LOG_DOSE -> LogDoseStep(
                    medicationName = uiState.medicationName,
                    onTaken = viewModel::logDoseNow,
                    onLater = viewModel::skipLogDose
                )
                OnboardingStep.ADD_DETAILS -> AddDetailsStep(
                    onFinish = viewModel::finishOnboarding
                )
            }
        }
    }
}

@Composable
fun NameStep(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_name_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.onboarding_name_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onNext,
        enabled = name.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.action_continue))
    }
}

@Composable
fun LogDoseStep(
    medicationName: String,
    onTaken: () -> Unit,
    onLater: () -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_log_title, medicationName),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = onTaken,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.onboarding_action_taken))
    }
    Spacer(modifier = Modifier.height(12.dp))
    TextButton(
        onClick = onLater,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.onboarding_action_later))
    }
}

@Composable
fun AddDetailsStep(onFinish: () -> Unit) {
    Text(
        text = stringResource(R.string.onboarding_details_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_details_message),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = onFinish, // Simplified for Step 4
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.onboarding_action_details))
    }
    Spacer(modifier = Modifier.height(12.dp))
    TextButton(
        onClick = onFinish,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.onboarding_action_not_now))
    }
}
