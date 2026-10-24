package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TedApplication
import kotlinx.coroutines.launch

@Composable
fun MemoriesScreen(modifier: Modifier = Modifier) {
  val app = TedApplication.instance
  val repo = app.repository
  val memories by repo.allMemories.collectAsState(initial = emptyList())
  val scope = rememberCoroutineScope()

  var showAddDialog by remember { mutableStateOf(false) }
  var newKey by remember { mutableStateOf("") }
  var newValue by remember { mutableStateOf("") }
  var showClearConfirm by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
  ) {
    // Header
    ElevatedCard(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      modifier = Modifier.fillMaxWidth().testTag("memories_header")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Personalization & Memories",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Text(
          text = "Everything Ted knows about you to tailor his roasts and advice. You have 100% control.",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Action buttons: Add Memory & Clear All
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = { showAddDialog = true },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f).testTag("add_memory_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add Fact", fontSize = 13.sp)
      }

      OutlinedButton(
        onClick = { showClearConfirm = true },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD34538)),
        modifier = Modifier.weight(1f).testTag("clear_memories_button")
      ) {
        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Clear All", fontSize = 13.sp)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Privacy guarantee note
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF5A9E6F), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Strict local storage: No raw audio recordings are kept.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // List of Memories
    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (memories.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text("No saved memories yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                "Ted hasn't saved any facts about you yet. Tap 'Add Fact' above to teach him your nickname, job, or hobbies so his roasts get savage.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      items(memories, key = { it.id }) { mem ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth().testTag("memory_item_${mem.id}")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = mem.key,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFE89A3C)
              )
              Text(
                text = mem.value,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            IconButton(
              onClick = { scope.launch { repo.deleteMemory(mem.id) } },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color(0xFFD34538).copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  }

  // Add Memory Dialog
  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Add Personal Detail for Ted") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = newKey,
            onValueChange = { newKey = it },
            label = { Text("Topic (e.g. Job, Coffee, Favorite Game)") },
            singleLine = true
          )
          OutlinedTextField(
            value = newValue,
            onValueChange = { newValue = it },
            label = { Text("Detail (e.g. Android dev who drinks cold brew)") },
            singleLine = true
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newKey.isNotBlank() && newValue.isNotBlank()) {
              scope.launch {
                repo.addMemory(newKey, newValue)
                newKey = ""
                newValue = ""
                showAddDialog = false
              }
            }
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Clear All Confirm Dialog
  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text("Clear All Memories?") },
      text = {
        Text("Ted will forget all saved preferences, nicknames, and personal facts. This cannot be undone.")
      },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              repo.clearMemories()
              showClearConfirm = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD34538))
        ) {
          Text("Clear Everything")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
