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

@Composable
fun S3Form(
    s3input: AddRemoteRepositoryDialog.Input.S3,
    isEditable: Boolean,
) {
    Text(
        "Connection details for S3 bucket:",
    )
    Spacer(Modifier.height(4.dp))
    TextField(
        s3input.endpoint.value,
        onValueChange = { s3input.endpoint.value = it },
        label = { Text("Endpoint URL") },
        placeholder = { Text("Endpoint URL") },
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
                    "Insecure protocol is used for endpoint. " +
                        "This results in plain data being sent over network, that is readable by anyone.",
                )
                Spacer(Modifier.height(8.dp))
                Text("It is strongly recommended to connect to this server using a VPN you absolutely trust.")
            }
        }
    }
    TextField(
        s3input.bucket.value,
        onValueChange = { s3input.bucket.value = it },
        label = { Text("Bucket name") },
        placeholder = { Text("Bucket name") },
        singleLine = true,
        enabled = isEditable,
        modifier = Modifier.fillMaxWidth(),
    )
    TextField(
        s3input.accessKey.value,
        onValueChange = { s3input.accessKey.value = it },
        label = { Text("Access key") },
        placeholder = { Text("Access key") },
        singleLine = true,
        enabled = isEditable,
        modifier = Modifier.fillMaxWidth(),
    )
    PasswordField(
        s3input.secretKey.value,
        onValueChange = { s3input.secretKey.value = it },
        label = { Text("Secret key") },
        placeholder = { Text("Secret key") },
        enabled = isEditable,
        modifier = Modifier.fillMaxWidth(),
    )
    CheckboxWithText(
        s3input.rememberCredentials.value,
        onValueChange = {
            s3input.rememberCredentials.value = it
        },
        text = "Remember credentials",
    )

    Spacer(Modifier.height(12.dp))
}
