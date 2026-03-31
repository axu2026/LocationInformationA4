package com.example.locationinformation

import android.location.Location
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.locationinformation.ui.theme.LocationInformationTheme
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.*
import android.location.Geocoder
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

        locationCallback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    setContent {
                        LocationInformationTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                MapScreen(location)
                            }
                        }
                    }
                }
            }
        }
        setContent {
            LocationInformationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    RequestPermissions(fusedLocationClient, locationRequest, locationCallback)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, null
            )
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

@Composable
fun RequestPermissions(
    fusedLocationClient: FusedLocationProviderClient,
    locationRequest: LocationRequest,
    locationCallback: LocationCallback
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncer = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        if (isGranted) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, null
                )
            } catch (e: SecurityException) {
                Log.e("Location", "Security Exception: ${e.message}")
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            requestPermissionLauncer.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, null
                )
            } catch (e: SecurityException) {
                Log.e("Location", "Security Exception: ${e.message}")
            }
        }
    }
}

@Composable
fun MapScreen(location: Location) {
    val context = LocalContext.current

    // create coordinates from user location
    val coordinates = LatLng(location.latitude, location.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.Builder()
            .target(coordinates)
            .zoom(25f)
            .bearing(70f)
            .tilt(80f)
            .build()
    }

    var properties by remember {
        mutableStateOf(MapProperties(mapType = MapType.SATELLITE))
    }

    var uiSettings by remember {
        mutableStateOf(MapUiSettings(zoomControlsEnabled = false))
    }

    // the geocode address and its stringified version
    var rGeocodeAddress by remember { mutableStateOf<Address?>(null) }
    var stringAddress by remember { mutableStateOf("") }
    // list of custom markers made by the user
    val customMarkers = remember { mutableStateListOf<LatLng>() }

    // create the string address to be displayed as info
    LaunchedEffect(location) {
        rGeocodeAddress = reverseGeocode(context, location.latitude, location.longitude)
        stringAddress = formatAddress(rGeocodeAddress)
    }

    Box {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = properties,
            uiSettings = uiSettings,
            onMapClick = { latLng ->
                customMarkers.add(latLng)
            }
        ) {
            Marker(
                state = MarkerState(position = coordinates),
                title = "Your Location",
                snippet = "${location.latitude}, ${location.longitude}"
            )
            customMarkers.forEach { marker ->
                Marker(
                    state = MarkerState(position = marker),
                    title = "Custom Marker",
                    snippet = "${marker.latitude}, ${marker.longitude}"
                )
            }
        }
        Card(
            modifier = Modifier.padding(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
            ) {
                Text(text = "Address Information:", fontWeight = FontWeight.Bold)
                Text(text = stringAddress)
                Text(text = "${location.latitude}, ${location.longitude}")
            }
        }
    }
}

// function to obtain the reverse geocode
suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): Address? =
    withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context)
        try {
            val addresses = geocoder.getFromLocation(
                lat,
                lon,
                1
            )
            addresses?.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

// function turning the result of the reverse geocode into string version of address
fun formatAddress(address: Address?): String {
    return address?.let {
        val addressLines = (0..it.maxAddressLineIndex).mapNotNull { i ->
            it.getAddressLine(i)
        }
        addressLines.joinToString(separator = ", ")
    } ?: "no address found"
}