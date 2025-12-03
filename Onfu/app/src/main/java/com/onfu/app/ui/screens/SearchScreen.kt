package com.onfu.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.onfu.app.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

@Composable
fun SearchScreen() {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchedUser>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var followingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedUser by remember { mutableStateOf<SearchedUser?>(null) }
    val followLoading = remember { mutableStateMapOf<String, Boolean>() }

    val currentUid = auth.currentUser?.uid

    LaunchedEffect(currentUid) {
        if (currentUid == null) {
            followingIds = emptySet()
            return@LaunchedEffect
        }
        try {
            val snapshot = firestore.collection("users").document(currentUid)
                .collection("following")
                .get()
                .await()
            followingIds = snapshot.documents.map { it.id }.toSet()
        } catch (t: Throwable) {
            errorMessage = t.localizedMessage
        }
    }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            results = emptyList()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        errorMessage = null
        try {
            val snapshot = firestore.collection("users")
                .orderBy("userid")
                .startAt(trimmed)
                .endAt("$trimmed\uf8ff")
                .limit(30)
                .get()
                .await()
            results = snapshot.documents.mapNotNull { doc ->
                val uid = doc.id
                val username = doc.getString("userid") ?: doc.getString("username") ?: uid
                val displayName = doc.getString("visibleName")
                    ?: doc.getString("displayName")
                    ?: username
                val photo = doc.getString("photoUrl")
                    ?: doc.getString("avatarUrl")
                    ?: ""
                val bio = doc.getString("bio") ?: ""
                val typeFlag = doc.getString("userType")?.lowercase(Locale.US) ?: ""
                val explicitVerified = doc.getBoolean("isVerified") ?: doc.getBoolean("verified") ?: false
                val isVerified = explicitVerified || typeFlag == "verified"
                SearchedUser(uid, username, displayName, photo, bio, isVerified)
            }.filter { it.uid != auth.currentUser?.uid }
        } catch (t: Throwable) {
            errorMessage = t.localizedMessage
            results = emptyList()
        } finally {
            loading = false
        }
    }

    fun refreshFollowing() {
        val uid = currentUid ?: return
        coroutineScope.launch {
            try {
                val snapshot = firestore.collection("users").document(uid)
                    .collection("following")
                    .get()
                    .await()
                followingIds = snapshot.documents.map { it.id }.toSet()
            } catch (t: Throwable) {
                errorMessage = t.localizedMessage
            }
        }
    }

    fun toggleFollow(targetId: String, shouldFollow: Boolean) {
        val uid = currentUid ?: return
        coroutineScope.launch {
            followLoading[targetId] = true
            try {
                val batch = firestore.batch()
                val followerRef = firestore.collection("users").document(targetId)
                    .collection("followers")
                    .document(uid)
                val followingRef = firestore.collection("users").document(uid)
                    .collection("following")
                    .document(targetId)
                if (shouldFollow) {
                    batch.set(followerRef, mapOf("since" to FieldValue.serverTimestamp()))
                    batch.set(followingRef, mapOf("since" to FieldValue.serverTimestamp()))
                } else {
                    batch.delete(followerRef)
                    batch.delete(followingRef)
                }
                batch.commit().await()
                refreshFollowing()
            } catch (t: Throwable) {
                errorMessage = t.localizedMessage
            } finally {
                followLoading[targetId] = false
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "Buscar por usuario (@handle)") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            singleLine = true,
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Limpiar")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            loading -> CenteredLoading()
            query.trim().length < 2 -> Text("Escribe al menos 2 caracteres para buscar", color = Color.Gray)
            errorMessage != null -> Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            results.isEmpty() -> Text("No hay resultados", color = Color.Gray)
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results) { user ->
                    val isFollowing = followingIds.contains(user.uid)
                    SearchResultRow(
                        user = user,
                        isFollowing = isFollowing,
                        isLoading = followLoading[user.uid] == true,
                        canFollow = currentUid != null && currentUid != user.uid,
                        onFollowClick = { toggleFollow(user.uid, !isFollowing) },
                        onRowClick = { selectedUser = user }
                    )
                    Divider()
                }
            }
        }
    }

    selectedUser?.let { user ->
        ProfileDialog(
            user = user,
            isFollowing = followingIds.contains(user.uid),
            isFollowingLoading = followLoading[user.uid] == true,
            onFollowToggle = { toggleFollow(user.uid, !followingIds.contains(user.uid)) },
            onDismiss = { selectedUser = null }
        )
    }
}

@Composable
private fun SearchResultRow(
    user: SearchedUser,
    isFollowing: Boolean,
    isLoading: Boolean,
    canFollow: Boolean,
    onFollowClick: () -> Unit,
    onRowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = "Avatar de ${user.displayName}",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable { onRowClick() },
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onRowClick() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.displayName, fontWeight = FontWeight.SemiBold)
                if (user.isVerified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_verified_badge),
                        contentDescription = stringResource(id = R.string.verified_badge),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = "@${user.username}",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (canFollow) {
            Button(
                onClick = onFollowClick,
                enabled = !isLoading,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(if (isFollowing) "Siguiendo" else "Seguir")
                }
            }
        }
    }
}

@Composable
private fun ProfileDialog(
    user: SearchedUser,
    isFollowing: Boolean,
    isFollowingLoading: Boolean,
    onFollowToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var followersCount by remember { mutableStateOf<Int?>(null) }
    var followingCount by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(user.uid) {
        try {
            val followerSnapshot = firestore.collection("users").document(user.uid)
                .collection("followers")
                .get()
                .await()
            followersCount = followerSnapshot.size()
        } catch (t: Throwable) {
            followersCount = null
        }
        try {
            val followingSnapshot = firestore.collection("users").document(user.uid)
                .collection("following")
                .get()
                .await()
            followingCount = followingSnapshot.size()
        } catch (t: Throwable) {
            followingCount = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = "Avatar de ${user.displayName}",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user.displayName, fontWeight = FontWeight.Bold)
                            if (user.isVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.ic_verified_badge),
                                    contentDescription = stringResource(id = R.string.verified_badge),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text("@${user.username}", color = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (user.bio.isBlank()) "Sin descripción" else user.bio,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatColumn(label = "Seguidores", value = followersCount)
                    StatColumn(label = "Siguiendo", value = followingCount)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onFollowToggle,
                    enabled = !isFollowingLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isFollowingLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(if (isFollowing) "Dejar de seguir" else "Seguir")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value?.toString() ?: "-",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium
        )
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CenteredLoading() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

data class SearchedUser(
    val uid: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val bio: String,
    val isVerified: Boolean
)