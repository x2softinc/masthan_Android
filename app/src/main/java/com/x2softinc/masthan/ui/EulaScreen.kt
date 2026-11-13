package com.x2softinc.masthan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x2softinc.masthan.AppMetadata

// TEMPORARY in-app Terms of Use (EULA), mirroring Views/EULAView.swift.
//
// NOTE: The text below is a template and must be reviewed by the developer / legal counsel
// before shipping. Fill in the bracketed placeholders. The Apple-specific wording of the iOS
// template (Apple-branded devices, Apple Media Services) is adapted to Android / Google Play.

private class EulaSection(val heading: String, val paragraphs: List<String>)

private val sections = listOf(
    EulaSection(
        "1. License",
        listOf(
            "Subject to the terms of this Agreement, X2Soft Inc. grants you a limited, non-exclusive, non-transferable, revocable license to install and use Masthan Gani Masjid (the App) on Android devices that you own or control, solely for your personal, non-commercial use.",
            "Google LLC is not a party to this Agreement and has no obligation to provide maintenance or support for the App. This license is also subject to the Google Play Terms of Service.",
        ),
    ),
    EulaSection(
        "2. Restrictions",
        listOf(
            "You agree not to copy, modify, translate, reverse engineer, decompile, or disassemble the App, or otherwise attempt to derive its source code, except to the extent this restriction is expressly prohibited by applicable law.",
            "You may not rent, lease, lend, sell, sublicense, or redistribute the App, and you may not remove or alter any copyright, trademark, or other proprietary notices. You agree not to use the App to violate any applicable law or regulation.",
        ),
    ),
    EulaSection(
        "3. Your Account and the MasjidPro / Masthan Gani Masjid Service",
        listOf(
            "The App displays prayer, Iqama, and announcement information provided by a masjid through its MasjidPro / Masthan Gani Masjid account. You are responsible for keeping any device code or credentials associated with the App confidential and for all activity performed through them.",
            "Prayer and Iqama times are provided for convenience. Times may vary by calculation method and location, and you should verify them with your local masjid. The developer does not guarantee their accuracy.",
        ),
    ),
    EulaSection(
        "4. Privacy and Data",
        listOf(
            "Your use of the App is subject to our Privacy Policy, which explains what information is collected, how it is used, and the choices you have. By using the App you consent to the practices described there.",
        ),
    ),
    EulaSection(
        "5. Content and Third Parties",
        listOf(
            "The App may include audio content (such as Adhan recitations) and links to third-party websites or services. Such third-party materials remain the property of their respective owners and are subject to their own terms. The developer is not responsible for third-party content or services.",
        ),
    ),
    EulaSection(
        "6. Updates and Termination",
        listOf(
            "The developer may update, modify, or discontinue the App or any feature at any time. This license remains in effect until terminated. It terminates automatically if you breach any term of this Agreement. You may stop using the App at any time.",
        ),
    ),
    EulaSection(
        "7. Disclaimer of Warranties",
        listOf(
            "TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, THE APP IS PROVIDED AS IS AND AS AVAILABLE, WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS, IMPLIED, OR STATUTORY, INCLUDING WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT.",
        ),
    ),
    EulaSection(
        "8. Limitation of Liability",
        listOf(
            "TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, X2Soft INC. SHALL NOT BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES, OR FOR ANY LOSS OF PROFITS, REVENUE, DATA, OR GOODWILL, ARISING OUT OF OR RELATED TO YOUR USE OR INABILITY TO USE THE APP. THE DEVELOPER'S TOTAL LIABILITY SHALL NOT EXCEED THE AMOUNT YOU PAID, IF ANY, FOR THE APP.",
        ),
    ),
    EulaSection(
        "9. Changes to This Agreement",
        listOf(
            "We may revise this Agreement from time to time. The most current version will always be shown here. If a revision is material, we will make reasonable efforts to notify you. By continuing to use the App after changes take effect, you accept the revised Agreement.",
        ),
    ),
    EulaSection(
        "10. Governing Law",
        listOf(
            "This Agreement is governed by the laws of [Jurisdiction], without regard to its conflict-of-law principles. Any dispute arising under this Agreement shall be subject to the exclusive jurisdiction of the courts located in [Jurisdiction].",
        ),
    ),
    EulaSection(
        "11. Contact",
        listOf("If you have any questions about this Agreement, please contact us at support@x2softinc.com."),
    ),
)

@Composable
fun EulaScreen() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("End User License Agreement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(AppMetadata.TERMS_OF_USE_TITLE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        sections.forEach { section ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(section.heading, style = MaterialTheme.typography.titleMedium)
                section.paragraphs.forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        HorizontalDivider()
        Text("Last updated: [Date]", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}
