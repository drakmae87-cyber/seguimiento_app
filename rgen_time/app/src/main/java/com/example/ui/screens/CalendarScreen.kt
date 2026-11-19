package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.CalendarEvent
import com.example.data.CalendarRepository
import com.example.data.EventCategory
import com.example.ui.components.ChamferedHudShape
import com.example.ui.components.CurvedSciFiHudBanner
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageCardElevated
import com.example.ui.theme.StageVoid
import com.example.ui.theme.parseHexColorOrNull
import com.example.model.TelemetryPushCategory
import com.example.viewmodel.CalendarUiState
import com.example.viewmodel.CalendarViewModel
import com.example.viewmodel.ClockFormatters
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarTabScreen(
    modifier: Modifier = Modifier,
    onLogAction: (TelemetryPushCategory, String, String) -> Unit = { _, _, _ -> }
) {
    val auth = remember { Firebase.auth }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptSilentGoogleSignIn(context, credentialManager, scope) {
                currentUser = auth.currentUser
            }
        }
    }

    val user = currentUser
    if (user == null) {
        CalendarAuthGate(
            onAuthSuccess = {
                currentUser = auth.currentUser
                onLogAction(TelemetryPushCategory.CALENDAR, "Sesión sincronizada", "Agenda Cloud conectada")
            },
            modifier = modifier
        )
    } else {
        key(user.uid) {
            val viewModel: CalendarViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        CalendarViewModel(CalendarRepository(db), user.uid)
                    }
                }
            )
            CalendarAuthenticatedScreen(
                userDisplayName = user.displayName ?: user.email ?: "Operador Corporativo",
                viewModel = viewModel,
                onLogAction = onLogAction,
                onSignOut = {
                    scope.launch {
                        withContext(NonCancellable) {
                            try {
                                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                            } catch (e: Exception) {
                                Log.w("Auth", "Failed to clear credential state: ${e.message}")
                            } finally {
                                auth.signOut()
                                onLogAction(TelemetryPushCategory.CALENDAR, "Sesión cerrada", "Agenda desconectada")
                            }
                        }
                    }
                },
                modifier = modifier
            )
        }
    }
}

private fun attemptSilentGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignedIn: () -> Unit
) {
    if (Firebase.auth.currentUser != null) {
        onSignedIn()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onSignedIn()
            }
        } catch (_: Exception) {
            // Silent auto sign-in unavailable; interactive button remains visible
        }
    }
}

private fun onInteractiveGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onError("Configuración de Google Sign-In faltante (default_web_client_id).")
        return
    }
    val activity = context as? Activity
    if (activity == null) {
        onError("Contexto de actividad no disponible.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onSuccess()
            } else {
                onError("Tipo de credencial inesperado.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onError(e.localizedMessage ?: "Error al iniciar sesión con Google.")
        }
    }
}

@Composable
private fun CalendarAuthGate(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val hudShape = remember { ChamferedHudShape(16.dp) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = hudShape,
            colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.88f)),
            border = BorderStroke(1.2.dp, palette.primaryGlow.copy(alpha = 0.6f)),
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(palette.primaryGlow.copy(alpha = 0.16f))
                        .border(1.5.dp, palette.primaryGlow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = palette.primaryGlow,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.calendar_auth_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkCream,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.calendar_auth_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center
                )

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF6B4A),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onInteractiveGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            scope = scope,
                            onSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onCancelled = {
                                isLoading = false
                            },
                            onError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = ChamferedHudShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryGlow,
                        contentColor = StageVoid
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = StageVoid,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.calendar_sign_in_google),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarAuthenticatedScreen(
    userDisplayName: String,
    viewModel: CalendarViewModel,
    onLogAction: (TelemetryPushCategory, String, String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val todayKey = remember { ClockFormatters.todayDateKey() }
    val initCal = remember { Calendar.getInstance() }
    var displayYear by remember { mutableIntStateOf(initCal.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(initCal.get(Calendar.MONTH)) }
    var selectedDateKey by remember { mutableStateOf(todayKey) }
    var showAddDialog by remember { mutableStateOf(false) }

    var ddQuery by remember {
        mutableStateOf(initCal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0'))
    }
    var mmQuery by remember {
        mutableStateOf((initCal.get(Calendar.MONTH) + 1).toString().padStart(2, '0'))
    }
    var aaQuery by remember {
        mutableStateOf((initCal.get(Calendar.YEAR) % 100).toString().padStart(2, '0'))
    }
    var dateJumpError by remember { mutableStateOf<String?>(null) }
    var searchKeyword by remember { mutableStateOf("") }

    val hudShape = remember { ChamferedHudShape(14.dp) }
    val chipShape = remember { ChamferedHudShape(8.dp) }

    val allEvents = (eventsState as? CalendarUiState.Success)?.events.orEmpty()
    val eventsByDate = remember(allEvents) { allEvents.groupBy { it.dateKey } }
    val dayEvents = remember(allEvents, selectedDateKey, searchKeyword) {
        val kw = searchKeyword.trim()
        if (kw.isNotBlank()) {
            allEvents.filter {
                it.title.contains(kw, ignoreCase = true) ||
                    it.note.contains(kw, ignoreCase = true) ||
                    it.dateKey.contains(kw, ignoreCase = true)
            }
        } else {
            allEvents.filter { it.dateKey == selectedDateKey }
        }
    }

    val monthTitle = remember(displayYear, displayMonth) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayYear)
            set(Calendar.MONTH, displayMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
            .format(c.time)
            .uppercase(Locale("es", "ES"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.calendar_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkCream
                    )
                    Text(
                        text = "SYNC // $userDisplayName",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryGlow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showAddDialog = true },
                        shape = chipShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryGlow,
                            contentColor = StageVoid
                        ),
                        modifier = Modifier.testTag("calendar_add_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.calendar_add_event))
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = chipShape,
                        border = BorderStroke(1.dp, StageBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkMuted),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.testTag("calendar_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = stringResource(R.string.calendar_sign_out),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            CurvedSciFiHudBanner(
                primaryText = monthTitle,
                secondaryText = "DÍA SELECCIONADO · $selectedDateKey"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Date Selector (DD / MM / AA) + Keyword Search
            Card(
                shape = hudShape,
                colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.84f)),
                border = BorderStroke(1.dp, palette.secondaryGlow.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ddQuery,
                            onValueChange = {
                                ddQuery = it.filter { c -> c.isDigit() }.take(2)
                                dateJumpError = null
                            },
                            label = { Text("DD", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primaryGlow,
                                unfocusedBorderColor = StageBorder,
                                focusedTextColor = InkCream,
                                unfocusedTextColor = InkCream
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calendar_jump_dd")
                        )
                        OutlinedTextField(
                            value = mmQuery,
                            onValueChange = {
                                mmQuery = it.filter { c -> c.isDigit() }.take(2)
                                dateJumpError = null
                            },
                            label = { Text("MM", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primaryGlow,
                                unfocusedBorderColor = StageBorder,
                                focusedTextColor = InkCream,
                                unfocusedTextColor = InkCream
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calendar_jump_mm")
                        )
                        OutlinedTextField(
                            value = aaQuery,
                            onValueChange = {
                                aaQuery = it.filter { c -> c.isDigit() }.take(4)
                                dateJumpError = null
                            },
                            label = { Text("AA", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primaryGlow,
                                unfocusedBorderColor = StageBorder,
                                focusedTextColor = InkCream,
                                unfocusedTextColor = InkCream
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("calendar_jump_aa")
                        )
                        Button(
                            onClick = {
                                val parsed = parseQuickDateDdMmAa(ddQuery, mmQuery, aaQuery)
                                if (parsed != null) {
                                    displayYear = parsed.first
                                    displayMonth = parsed.second
                                    selectedDateKey = ClockFormatters.formatDateKey(
                                        parsed.first,
                                        parsed.second,
                                        parsed.third
                                    )
                                    dateJumpError = null
                                } else {
                                    dateJumpError = "Fecha DD/MM/AA inválida"
                                }
                            },
                            shape = chipShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryGlow,
                                contentColor = StageVoid
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("calendar_jump_go")
                        ) {
                            Text("Ir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val now = Calendar.getInstance()
                                displayYear = now.get(Calendar.YEAR)
                                displayMonth = now.get(Calendar.MONTH)
                                selectedDateKey = ClockFormatters.todayDateKey()
                                ddQuery = now.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                                mmQuery = (now.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                                aaQuery = (now.get(Calendar.YEAR) % 100).toString().padStart(2, '0')
                                dateJumpError = null
                            },
                            shape = chipShape,
                            border = BorderStroke(1.dp, palette.secondaryGlow.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Hoy", color = palette.secondaryGlow, fontSize = 11.sp)
                        }
                    }

                    if (!dateJumpError.isNullOrBlank()) {
                        Text(
                            text = dateJumpError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF6B4A)
                        )
                    }

                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        placeholder = { Text("Filtrar eventos por título, nota o fecha…", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.secondaryGlow,
                            unfocusedBorderColor = StageBorder,
                            focusedTextColor = InkCream,
                            unfocusedTextColor = InkCream
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_keyword_search")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month Grid Card
            Card(
                shape = hudShape,
                colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.84f)),
                border = BorderStroke(1.dp, StageBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (displayMonth == 0) {
                                    displayMonth = 11
                                    displayYear -= 1
                                } else {
                                    displayMonth -= 1
                                }
                            },
                            modifier = Modifier.testTag("calendar_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Mes anterior",
                                tint = InkCream
                            )
                        }
                        Text(
                            text = monthTitle,
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = palette.primaryGlow
                        )
                        IconButton(
                            onClick = {
                                if (displayMonth == 11) {
                                    displayMonth = 0
                                    displayYear += 1
                                } else {
                                    displayMonth += 1
                                }
                            },
                            modifier = Modifier.testTag("calendar_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Mes siguiente",
                                tint = InkCream
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("L", "M", "X", "J", "V", "S", "D").forEach { wd ->
                            Text(
                                text = wd,
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    CalendarMonthGrid(
                        year = displayYear,
                        monthZeroBased = displayMonth,
                        selectedDateKey = selectedDateKey,
                        todayKey = todayKey,
                        eventsByDate = eventsByDate,
                        onSelectDate = { selectedDateKey = it }
                    )
                }
            }

            if (!actionError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = actionError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF6B4A)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (val state = eventsState) {
                CalendarUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = palette.primaryGlow)
                    }
                }

                is CalendarUiState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFF6B4A),
                        modifier = Modifier.padding(12.dp)
                    )
                }

                is CalendarUiState.Success -> {
                    if (dayEvents.isEmpty()) {
                        Card(
                            shape = hudShape,
                            colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.65f)),
                            border = BorderStroke(1.dp, StageBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.calendar_empty_day),
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(dayEvents, key = { it.id }) { event ->
                                CalendarEventRowCard(
                                    event = event,
                                    onToggleCompleted = {
                                        viewModel.toggleCompleted(event.id, !event.completed)
                                        onLogAction(
                                            TelemetryPushCategory.CALENDAR,
                                            if (!event.completed) "Registro completado" else "Registro reabierto",
                                            event.title
                                        )
                                    },
                                    onDelete = {
                                        viewModel.deleteEvent(event.id)
                                        onLogAction(TelemetryPushCategory.CALENDAR, "Registro eliminado", event.title)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCalendarEventDialog(
            selectedDateKey = selectedDateKey,
            isSaving = isSaving,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, timeLabel, category, note ->
                viewModel.addEvent(
                    title = title,
                    dateKey = selectedDateKey,
                    timeLabel = timeLabel,
                    category = category,
                    note = note,
                    colorHex = category.defaultHex,
                    onAdded = {
                        showAddDialog = false
                        onLogAction(
                            TelemetryPushCategory.CALENDAR,
                            "Registro agendado · $selectedDateKey",
                            "$title${if (timeLabel.isNotBlank()) " ($timeLabel)" else ""}"
                        )
                    }
                )
            }
        )
    }
}

@Composable
private fun CalendarMonthGrid(
    year: Int,
    monthZeroBased: Int,
    selectedDateKey: String,
    todayKey: String,
    eventsByDate: Map<String, List<CalendarEvent>>,
    onSelectDate: (String) -> Unit
) {
    val palette = LocalNekoPalette.current
    val firstDayCal = remember(year, monthZeroBased) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Convert Sunday=1..Saturday=7 to Monday=0..Sunday=6
    val dayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)
    val leadingEmpty = (dayOfWeek + 5) % 7
    val totalCells = ((leadingEmpty + daysInMonth + 6) / 7) * 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (rowStart in 0 until totalCells step 7) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (offset in 0 until 7) {
                    val cellIdx = rowStart + offset
                    val dayNum = cellIdx - leadingEmpty + 1
                    if (dayNum in 1..daysInMonth) {
                        val dateKey = ClockFormatters.formatDateKey(year, monthZeroBased, dayNum)
                        val isSelected = dateKey == selectedDateKey
                        val isToday = dateKey == todayKey
                        val dayEventList = eventsByDate[dateKey].orEmpty()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.15f)
                                .clip(ChamferedHudShape(6.dp))
                                .background(
                                    when {
                                        isSelected -> palette.primaryGlow.copy(alpha = 0.25f)
                                        isToday -> palette.secondaryGlow.copy(alpha = 0.12f)
                                        else -> StageVoid.copy(alpha = 0.45f)
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.7.dp,
                                    color = when {
                                        isSelected -> palette.primaryGlow
                                        isToday -> palette.secondaryGlow.copy(alpha = 0.7f)
                                        else -> StageBorder.copy(alpha = 0.55f)
                                    },
                                    shape = ChamferedHudShape(6.dp)
                                )
                                .clickable { onSelectDate(dateKey) }
                                .testTag("calendar_day_$dayNum"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (isSelected) palette.primaryGlow else InkCream
                                )
                                if (dayEventList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        dayEventList.take(3).forEach { ev ->
                                            val dotColor = parseHexColorOrNull(ev.colorHex) ?: palette.secondaryGlow
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarEventRowCard(
    event: CalendarEvent,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = parseHexColorOrNull(event.colorHex) ?: LocalNekoPalette.current.primaryGlow
    val cat = EventCategory.fromId(event.category)
    val shape = remember { ChamferedHudShape(12.dp) }

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.86f)),
        border = BorderStroke(1.dp, accent.copy(alpha = if (event.completed) 0.25f else 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_event_${event.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleCompleted) {
                Icon(
                    imageVector = if (event.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Marcar completado",
                    tint = accent
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = ChamferedHudShape(4.dp),
                        color = accent.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = if (event.timeLabel.isNotBlank()) "${event.timeLabel} · ${cat.label}" else cat.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (event.completed) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (event.completed) InkMuted else InkCream
                )
                if (event.note.isNotBlank()) {
                    Text(
                        text = event.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Eliminar evento",
                    tint = InkMuted
                )
            }
        }
    }
}

@Composable
private fun AddCalendarEventDialog(
    selectedDateKey: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, timeLabel: String, category: EventCategory, note: String) -> Unit
) {
    val palette = LocalNekoPalette.current
    var title by remember { mutableStateOf("") }
    var timeLabel by remember { mutableStateOf("09:00") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EventCategory.OPERACIONES) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageCardElevated,
        titleContentColor = InkCream,
        textContentColor = InkCream,
        title = {
            Text(
                text = "AGENDAR · $selectedDateKey",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Título puntual *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryGlow,
                        unfocusedBorderColor = StageBorder,
                        focusedTextColor = InkCream,
                        unfocusedTextColor = InkCream
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_title_input")
                )

                OutlinedTextField(
                    value = timeLabel,
                    onValueChange = { timeLabel = it.take(10) },
                    label = { Text("Hora (ej. 14:30, opcional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryGlow,
                        unfocusedBorderColor = StageBorder,
                        focusedTextColor = InkCream,
                        unfocusedTextColor = InkCream
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(EventCategory.entries) { cat ->
                        val catColor = parseHexColorOrNull(cat.defaultHex) ?: palette.primaryGlow
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.25f),
                                selectedLabelColor = catColor,
                                containerColor = StageVoid,
                                labelColor = InkMuted
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(300) },
                    label = { Text("Nota rápida (opcional)") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryGlow,
                        unfocusedBorderColor = StageBorder,
                        focusedTextColor = InkCream,
                        unfocusedTextColor = InkCream
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, timeLabel, category, note) },
                enabled = title.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryGlow,
                    contentColor = StageVoid
                ),
                modifier = Modifier.testTag("calendar_confirm_add")
            ) {
                Text(if (isSaving) "Guardando…" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = InkMuted)
            }
        }
    )
}

/**
 * Parses DD / MM / AA (or AAAA) into Triple(fullYear, zeroBasedMonth, dayOfMonth).
 * Validates calendar bounds strictly (e.g. rejects 31/02/26).
 */
internal fun parseQuickDateDdMmAa(ddRaw: String, mmRaw: String, aaRaw: String): Triple<Int, Int, Int>? {
    val dd = ddRaw.trim().toIntOrNull() ?: return null
    val mm = mmRaw.trim().toIntOrNull() ?: return null
    val aaTrim = aaRaw.trim()
    val yearNum = aaTrim.toIntOrNull() ?: return null
    val fullYear = if (aaTrim.length <= 2) 2000 + yearNum else yearNum

    if (mm !in 1..12 || fullYear < 1970 || fullYear > 2199) return null

    val cal = Calendar.getInstance().apply {
        isLenient = false
        clear()
        set(Calendar.YEAR, fullYear)
        set(Calendar.MONTH, mm - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    if (dd !in 1..maxDay) return null
    return Triple(fullYear, mm - 1, dd)
}
