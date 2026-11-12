package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.elements.WarningAlert
import org.archivekeep.app.ui.components.designsystem.input.CheckboxWithText
import org.archivekeep.app.ui.components.designsystem.input.PasswordField
import org.archivekeep.app.ui.components.designsystem.input.TextField
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_remember_credentials
import org.archivekeep.ui.resources.dialog_add_remote_s3_access_key
import org.archivekeep.ui.resources.dialog_add_remote_s3_bucket
import org.archivekeep.ui.resources.dialog_add_remote_s3_connection
import org.archivekeep.ui.resources.dialog_add_remote_s3_endpoint
import org.archivekeep.ui.resources.dialog_add_remote_s3_insecure
import org.archivekeep.ui.resources.dialog_add_remote_s3_secret_key
import org.archivekeep.ui.resources.dialog_add_remote_s3_vpn_recommended
import org.jetbrains.compose.resources.stringResource

@Composable
fun S3Form(
    s3input: AddRemoteRepositoryDialog.Input.S3,
    isEditable: Boolean,
) {
    Text(
        stringResource(Res.string.dialog_add_remote_s3_connection),
    )
    Spacer(Modifier.height(4.dp))
    TextField(
        s3input.endpoint.value,
        onValueChange = { s3input.endpoint.value = it },
        label = { Text(stringResource(Res.string.dialog_add_remote_s3_endpoint)) },
        placeholder = { Text(stringResource(Res.string.dialog_add_remote_s3_endpoint)) },
        singleLine = true,
        enabled = isEditable,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Uri,
            ),
    )
    if (s3input.endpoint.value
            .trim()
            .startsWith("http://")
    ) {
        Spacer(Modifier.height(8.dp))
        WarningAlert {
            Column {
                Text(
                    stringResource(Res.string.dialog_add_remote_s3_insecure),
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.dialog_add_remote_s3_vpn_recommended))
            }
        }
    }
    TextField(
        s3input.bucket.value,
        onValueChange = { s3input.bucket.value = it },
        label = { Text(stringResource(Res.string.dialog_add_remote_s3_bucket)) },
        placeholder = { Text(stringResource(Res.string.dialog_add_remote_s3_bucket)) },
        singleLine = true,
        enabled = isEditable,
        modifier = Modifier.fillMaxWidth(),
    )
    TextField(
        s3input.accessKey.value,
        onValueChange = { s3input.accessKey.value = it },
        label = { Text(stringResource(Res.string.dialog_add_remote_s3_access_key)) },
        placeholder = { Text(stringResource(Res.string.dialog_add_remote_s3_access_key)) },
        singleLine = true,
        enabled = isEditable,
        modifier = Modifier.fillMaxWidth(),
    )
    PasswordField(
        s3input.secretKey.value,
        onValueChange = { s3input.secretKey.value = it },
        label = { Text(stringResource(Res.string.dialog_add_remote_s3_secret_key)) },
        placeholder = { Text(stringResource(Res.string.dialog_add_remote_s3_secret_key)) },
        enabled = isEditable,
        modifier = Modifier.fillMaxWidth(),
    )
    CheckboxWithText(
        s3input.rememberCredentials.value,
        onValueChange = {
            s3input.rememberCredentials.value = it
        },
        text = stringResource(Res.string.common_remember_credentials),
    )

    Spacer(Modifier.height(12.dp))
}
