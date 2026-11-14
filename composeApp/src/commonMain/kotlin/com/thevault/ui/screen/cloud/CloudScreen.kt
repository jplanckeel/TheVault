package com.thevault.ui.screen.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.Account
import com.thevault.domain.model.ShareScope
import com.thevault.ui.component.ButtonSize
import com.thevault.ui.component.ButtonVariant
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.LedColor
import com.thevault.ui.component.LinkText
import com.thevault.ui.component.Panel
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.Segment
import com.thevault.ui.component.SegmentedControl
import com.thevault.ui.component.StatusLed
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.VaultTextField
import com.thevault.ui.platform.rememberShareText
import com.thevault.ui.theme.Vault
import org.koin.compose.viewmodel.koinViewModel

/**
 * Account, backup / restore and read-only share links.
 *
 * @param onBack Back navigation.
 * @param viewModel Screen ViewModel.
 */
@Composable
fun CloudScreen(
    onBack: () -> Unit,
    viewModel: CloudViewModel = koinViewModel(),
) {
    val account by viewModel.account.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val shareText = rememberShareText()
    LaunchedEffect(account) { if (account != null) viewModel.loadLinks() }
    LaunchedEffect(state.created) {
        state.created?.let {
            shareText(it.url)
            viewModel.consumeCreated()
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(
            "Compte",
            eyebrow = "Sauvegarde et partage",
            leading = { KeyButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack) },
        )
        state.message?.let { msg ->
            StatusLed(
                if (state.error) {
                    LedColor.Power
                } else if (state.busy) {
                    LedColor.Indigo
                } else {
                    LedColor.Sync
                },
                label = msg,
                blink = state.busy,
            )
        }
        val a = account
        if (a == null) LoginPanel(viewModel) else SignedIn(a, state, viewModel, shareText)
    }
}

@Composable
private fun LoginPanel(viewModel: CloudViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Panel(Modifier.fillMaxWidth()) {
        Engrave("Connexion")
        Text(
            "Utilise ton compte du back-office TheVault pour sauvegarder la collection et créer des liens de partage.",
            style = Vault.type.caption,
            color = Vault.colors.inkMuted,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VaultTextField("Email", email, { email = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            PasswordField(password) { password = it }
            VaultButton(
                "Se connecter",
                { viewModel.login(email, password) },
                Modifier.fillMaxWidth(),
                enabled =
                    email.isNotBlank() && password.isNotBlank(),
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onChange: (String) -> Unit,
) {
    VaultTextField(
        "Mot de passe",
        value,
        onChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation(),
    )
}

@Composable
private fun SignedIn(
    account: Account,
    state: CloudUiState,
    viewModel: CloudViewModel,
    shareText: (String) -> Unit,
) {
    Panel(Modifier.fillMaxWidth()) {
        Row {
            Column(Modifier.weight(1f)) {
                Engrave("Connecté")
                Text(
                    account.name.ifBlank {
                        account.email
                    },
                    style = Vault.type.bodyStrong,
                    color = Vault.colors.ink,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            LinkText("Déconnexion", viewModel::logout)
        }
    }
    Panel(Modifier.fillMaxWidth()) {
        Engrave("Sauvegarde")
        Text(
            "Toute la collection, les envies et les listes, sur ton compte.",
            style = Vault.type.caption,
            color = Vault.colors.inkMuted,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VaultButton("Sauvegarder", viewModel::backupNow, Modifier.weight(1f), size = ButtonSize.Small, enabled = !state.busy)
            VaultButton(
                "Restaurer",
                viewModel::restoreNow,
                Modifier.weight(1f),
                ButtonVariant.Secondary,
                ButtonSize.Small,
                enabled = !state.busy,
            )
        }
    }
    SharePanel(state, viewModel, shareText)
}

@Composable
private fun SharePanel(
    state: CloudUiState,
    viewModel: CloudViewModel,
    shareText: (String) -> Unit,
) {
    var scope by rememberSaveable { mutableStateOf(ShareScope.COLLECTION) }
    var title by rememberSaveable { mutableStateOf("") }
    var hidePrices by rememberSaveable { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth()) {
        Engrave("Partage en lecture seule")
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SegmentedControl(listOf(Segment("Collection"), Segment("Envies")), scope.ordinal, { scope = ShareScope.entries[it] })
            VaultTextField("Titre", title, { title = it }, placeholder = "Ma collection GameCube")
            VaultChip("Masquer les prix", hidePrices, { hidePrices = !hidePrices })
            Text(
                "Les notes, prix d'achat, emplacements et prêts ne sont jamais partagés.",
                style = Vault.type.caption,
                color = Vault.colors.inkMuted,
            )
            VaultButton(
                "Créer et partager le lien",
                { viewModel.share(scope, title, hidePrices) },
                Modifier.fillMaxWidth(),
                enabled = !state.busy,
            )
        }
        state.links.forEach { link ->
            Row(Modifier.padding(top = 14.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        link.title.ifBlank {
                            if (link.scope ==
                                ShareScope.WISHLIST
                            ) {
                                "Envies"
                            } else {
                                "Collection"
                            }
                        },
                        style = Vault.type.bodyStrong,
                        color = Vault.colors.ink,
                    )
                    Text(link.url, style = Vault.type.monoTag, color = Vault.colors.inkFaint, maxLines = 1)
                }
                LinkText("Partager", { shareText(link.url) }, Modifier.padding(start = 8.dp))
                LinkText("Révoquer", { viewModel.revoke(link) }, Modifier.padding(start = 12.dp))
            }
        }
    }
}
