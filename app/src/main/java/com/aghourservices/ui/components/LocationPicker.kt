package com.aghourservices.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aghourservices.R
import com.aghourservices.ui.theme.Aghour

/**
 * Default center coordinates for Aghour Al-Kubra (أجهور الكبرى), Qalyubia, Egypt.
 */
const val AGHOUR_DEFAULT_LAT = 30.3601
const val AGHOUR_DEFAULT_LNG = 31.2173

/**
 * Utility for parsing coordinates from text, links, or geo URIs.
 */
object LocationUtils {
    // Regex matching latitude and longitude numbers in text or URLs
    private val COORD_REGEX = Regex("""(-?\d{1,2}\.\d+)\s*[,;\s]\s*(-?\d{1,3}\.\d+)""")

    fun parseCoordinates(input: String): Pair<Double, Double>? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val match = COORD_REGEX.find(trimmed) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lng = match.groupValues[2].toDoubleOrNull() ?: return null

        return if (lat in -90.0..90.0 && lng in -180.0..180.0) {
            Pair(lat, lng)
        } else {
            null
        }
    }
}

/**
 * Reusable Location Picker component for firm creation and editing.
 * Replaces manual latitude/longitude numeric text fields with a user-friendly map-based interface:
 * 1. Visual status card showing current coordinates and "Show on Map" preview.
 * 2. GPS "Use My Current Location" button with runtime permissions.
 * 3. "Open Maps App" launcher for pin dropping and location discovery.
 * 4. Smart parser field for pasting Google Maps links or raw coordinates.
 * 5. Expandable manual coordinate override.
 */
@SuppressLint("MissingPermission")
@Composable
fun LocationPicker(
    latitude: String,
    longitude: String,
    onCoordinatesChanged: (lat: String, lng: String) -> Unit,
    modifier: Modifier = Modifier,
    error: Int? = null,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val c = Aghour.colors

    var showManual by rememberSaveable { mutableStateOf(false) }
    var pasteInput by rememberSaveable { mutableStateOf("") }
    var pasteError by rememberSaveable { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }

    val currentLat = latitude.trim().toDoubleOrNull()
    val currentLng = longitude.trim().toDoubleOrNull()
    val hasValidLocation = currentLat != null && currentLng != null

    val fetchGpsLocation: () -> Unit = {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            Toast.makeText(context, R.string.location_permission_required, Toast.LENGTH_SHORT).show()
        } else {
            isFetchingLocation = true
            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                isFetchingLocation = false
                Toast.makeText(context, R.string.location_permission_required, Toast.LENGTH_SHORT).show()
            } else {
                val lastKnownGps = runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
                val lastKnownNet = runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
                val bestLast = lastKnownGps ?: lastKnownNet

                if (bestLast != null) {
                    onCoordinatesChanged(
                        "%.6f".format(java.util.Locale.US, bestLast.latitude),
                        "%.6f".format(java.util.Locale.US, bestLast.longitude),
                    )
                    isFetchingLocation = false
                } else {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            onCoordinatesChanged(
                                "%.6f".format(java.util.Locale.US, loc.latitude),
                                "%.6f".format(java.util.Locale.US, loc.longitude),
                            )
                            isFetchingLocation = false
                            lm.removeUpdates(this)
                        }
                    }
                    val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
                    runCatching {
                        @Suppress("DEPRECATION")
                        lm.requestSingleUpdate(provider, listener, null)
                    }.onFailure {
                        isFetchingLocation = false
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchGpsLocation()
        } else {
            Toast.makeText(context, R.string.location_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    val onCurrentLocationClick: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            fetchGpsLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    val applyPastedText: (String) -> Unit = { text ->
        val parsed = LocationUtils.parseCoordinates(text)
        if (parsed != null) {
            onCoordinatesChanged(
                "%.6f".format(java.util.Locale.US, parsed.first),
                "%.6f".format(java.util.Locale.US, parsed.second),
            )
            pasteInput = ""
            pasteError = false
            focusManager.clearFocus()
        } else {
            pasteError = true
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.firm_location),
            style = MaterialTheme.typography.labelMedium,
            color = c.muted,
        )
        Spacer(Modifier.height(6.dp))

        // Main Location Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface)
                .border(
                    width = 1.dp,
                    color = if (error != null) c.bad else c.line,
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(14.dp),
        ) {
            // Location Status Banner
            if (hasValidLocation && currentLat != null && currentLng != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = c.ok,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.location_set),
                            style = MaterialTheme.typography.titleSmall,
                            color = c.ink,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${"%.5f".format(java.util.Locale.US, currentLat)}, ${"%.5f".format(java.util.Locale.US, currentLng)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.muted,
                        )
                    }
                    TextButton(
                        onClick = { onCoordinatesChanged("", "") },
                    ) {
                        Text(
                            text = stringResource(R.string.clear_location),
                            color = c.bad,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Preview on Map Action Button
                OutlinedButton(
                    onClick = {
                        context.openMap(currentLat, currentLng, "موقع المنشأة")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = c.goldInk),
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.preview_on_map), style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = c.muted,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.location_not_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.muted,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons Row: GPS Location & Open External Maps App
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Button 1: GPS Current Location
                Button(
                    onClick = onCurrentLocationClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.surface2,
                        contentColor = c.ink,
                    ),
                    enabled = !isFetchingLocation,
                ) {
                    if (isFetchingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = c.gold,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp), tint = c.gold)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.use_current_location),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }

                // Button 2: Open External Map App
                OutlinedButton(
                    onClick = {
                        val targetLat = currentLat ?: AGHOUR_DEFAULT_LAT
                        val targetLng = currentLng ?: AGHOUR_DEFAULT_LNG
                        context.openMap(targetLat, targetLng, "اختر موقع المنشأة")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = c.ink),
                ) {
                    Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(16.dp), tint = c.gold)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.open_map_picker),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Paste Google Maps Link or Coordinates Input
            Column {
                Text(
                    text = stringResource(R.string.paste_location_link),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted,
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = pasteInput,
                    onValueChange = {
                        pasteInput = it
                        pasteError = false
                        if (it.isNotBlank()) {
                            applyPastedText(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            stringResource(R.string.paste_location_placeholder),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.muted.copy(alpha = 0.6f),
                        )
                    },
                    singleLine = true,
                    isError = pasteError,
                    supportingText = if (pasteError) {
                        { Text(stringResource(R.string.invalid_location_pasted), color = c.bad) }
                    } else null,
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        if (pasteInput.isNotBlank()) {
                            TextButton(onClick = { applyPastedText(pasteInput) }) {
                                Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp), tint = c.gold)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { applyPastedText(pasteInput) },
                    ),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = c.surface2,
                        unfocusedContainerColor = c.surface2,
                        focusedTextColor = c.ink,
                        unfocusedTextColor = c.ink,
                        focusedBorderColor = c.gold,
                        unfocusedBorderColor = c.line,
                        errorBorderColor = c.bad,
                    ),
                )
            }

            // Expandable Manual Coordinates Override
            TextButton(
                onClick = { showManual = !showManual },
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(
                    text = stringResource(R.string.manual_coordinates),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted,
                )
            }

            AnimatedVisibility(visible = showManual) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FormField(
                        label = stringResource(R.string.latitude),
                        value = latitude,
                        onChange = { v -> onCoordinatesChanged(v, longitude) },
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal,
                        ltr = true,
                    )
                    FormField(
                        label = stringResource(R.string.longitude),
                        value = longitude,
                        onChange = { v -> onCoordinatesChanged(latitude, v) },
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal,
                        ltr = true,
                    )
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(error),
                style = MaterialTheme.typography.bodySmall,
                color = c.bad,
            )
        }
    }
}
