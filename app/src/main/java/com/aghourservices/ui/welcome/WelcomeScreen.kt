package com.aghourservices.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aghourservices.R
import com.aghourservices.ui.components.GoldButton
import com.aghourservices.ui.components.StatusBarIcons
import com.aghourservices.ui.theme.Cream
import com.aghourservices.ui.theme.Forest
import com.aghourservices.ui.theme.Gold
import com.aghourservices.ui.theme.Tajawal

/** First launch only: the splash artwork with a "start" button between the logo and the village skyline. */
@Composable
fun WelcomeScreen(onStart: () -> Unit, onLogin: () -> Unit) {
    StatusBarIcons(overPhoto = true)
    BoxWithConstraints(Modifier.fillMaxSize().background(Forest)) {
        Image(
            painter = painterResource(R.drawable.welcome_splash),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = maxHeight * 0.60f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.welcome_title),
                color = Cream.copy(alpha = 0.92f),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = Tajawal,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            GoldButton(stringResource(R.string.start_now), onStart, fontFamily = Tajawal, modifier = Modifier.height(54.dp))
            TextButton(onClick = onLogin) {
                Text(stringResource(R.string.have_account_login), color = Gold, fontFamily = Tajawal, fontWeight = FontWeight.Bold)
            }
        }
    }
}
