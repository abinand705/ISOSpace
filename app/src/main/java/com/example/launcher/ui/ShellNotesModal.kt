package com.example.launcher.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// Ubuntu design guidelines
private val UbuntuPurple = Color(0xFF2C001E)
private val UbuntuAubergine = Color(0xFF5E2750)
private val UbuntuWarmOrange = Color(0xFFE95420)
private val UbuntuDarkSlate = Color(0xFF222222)
private val UbuntuCardGray = Color(0xFF2D2D2D)

data class NotesAppItem(
    val id: String,
    val title: String,
    val content: String,
    val dateModified: Long,
    val category: String, // "Personal", "Work", "Ideas", "System"
    val colorHex: String // Custom sticky note colors
) : java.io.Serializable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShellNotesModal(onClose: () -> Unit) {
    val context = LocalContext.current

    // Notes state
    var notesList by rememberSaveable { mutableStateOf<List<NotesAppItem>>(emptyList()) }
    var selectedNote by rememberSaveable { mutableStateOf<NotesAppItem?>(null) }
    var noteTitle by rememberSaveable { mutableStateOf("") }
    var noteContent by rememberSaveable { mutableStateOf("") }
    var noteCategory by rememberSaveable { mutableStateOf("Personal") }
    var noteColorHex by rememberSaveable { mutableStateOf("#FF9800") } // Warm sticky orange default

    // UI state filters
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("All") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isEditingMode by rememberSaveable { mutableStateOf(false) } // Is Editor pane open

    // Load list initially
    LaunchedEffect(Unit) {
        notesList = loadSavedNotes(context)
        if (notesList.isEmpty()) {
            // Seed a few beautiful ISOSpace workspace custom notes
            notesList = listOf(
                NotesAppItem(
                    id = "seed_1",
                    title = "ISOSpace Launcher Tips",
                    content = "1. Double tap desktop widgets to bring up action settings.\n2. Add/remove physical system apps via Terminal commands. Run 'apps help' or 'apps list-installed' inside Terminal to manage packages.\n3. Swipe left to launch secondary search lens.\n4. Use the custom Shotwell Gallery to review hardware captures.",
                    dateModified = System.currentTimeMillis() - 36000000,
                    category = "System",
                    colorHex = "#E95420"
                ),
                NotesAppItem(
                    id = "seed_2",
                    title = "Terminal Shortcuts",
                    content = "Remember to execute common commands in our custom desktop workspace terminal:\n- 'help' lists built-in system tools\n- 'apps' manages external/physical system applications\n- 'clear' drains terminal buffer\n- 'matrix' triggers green binary raindrops cascade",
                    dateModified = System.currentTimeMillis() - 72000000,
                    category = "Ideas",
                    colorHex = "#4CAF50"
                )
            )
            saveNotesList(context, notesList)
        }
    }

    // Filter list
    val filteredNotes = remember(notesList, selectedCategoryFilter, searchQuery) {
        var res = notesList
        if (selectedCategoryFilter != "All") {
            res = res.filter { it.category == selectedCategoryFilter }
        }
        if (searchQuery.isNotBlank()) {
            res = res.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true)
            }
        }
        res.sortedByDescending { it.dateModified }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEC0E0E12)),
            border = BorderStroke(
                width = 1.5.dp,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            ),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("notes_app_dialog")
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isCompact = maxWidth < 560.dp
                Column(modifier = Modifier.fillMaxSize()) {
                    // Toolbar Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(UbuntuPurple.copy(alpha = 0.45f), UbuntuAubergine.copy(alpha = 0.15f))))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = UbuntuWarmOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notes & Sticky Ledger",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Desktop Rich Sticky Document Editor",
                                color = Color.LightGray.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }

                        // Floating close button
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp).testTag("close_notes_button")) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Sub header: Filter and Categorization Tabs (responsive stacking)
                    if (isCompact) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Category Filters Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth()
                            ) {
                                listOf("All", "Personal", "Work", "Ideas", "System").forEach { cat ->
                                    val isSel = selectedCategoryFilter == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSel) UbuntuWarmOrange else Color(0xFF333333))
                                            .clickable { selectedCategoryFilter = cat }
                                            .padding(vertical = 4.dp, horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Search notes
                            CompactSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Filter notes...",
                                borderColor = UbuntuWarmOrange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Category Filters Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                listOf("All", "Personal", "Work", "Ideas", "System").forEach { cat ->
                                    val isSel = selectedCategoryFilter == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSel) UbuntuWarmOrange else Color(0xFF333333))
                                            .clickable { selectedCategoryFilter = cat }
                                            .padding(vertical = 4.dp, horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Search notes
                            CompactSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Filter notes...",
                                borderColor = UbuntuWarmOrange,
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(34.dp)
                            )
                        }
                    }

                    // Workspace Body (Split Pane or Single Pane on mobile)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF151515))
                    ) {
                        // LEFT COLUMN: Notes list
                        if (!isCompact || !isEditingMode) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(if (isCompact) 1f else 0.45f)
                                    .drawBehind { 
                                        if (!isCompact) {
                                            drawLine(Color(0x1FFFFFFF), Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = 1.dp.toPx()) 
                                        }
                                    }
                                    .padding(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectedNote = null
                                        noteTitle = ""
                                        noteContent = ""
                                        noteCategory = "Personal"
                                        noteColorHex = "#E95420"
                                        isEditingMode = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = UbuntuWarmOrange),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("New Sticky Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (filteredNotes.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No notes in ledger", color = Color.DarkGray, fontSize = 12.sp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(filteredNotes) { item ->
                                            val isSelected = selectedNote?.id == item.id
                                            val stickyColor = try {
                                                Color(android.graphics.Color.parseColor(item.colorHex))
                                            } catch (e: Exception) {
                                                Color(0xFFE95420)
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) stickyColor.copy(alpha = 0.25f)
                                                        else Color(0xFF222222)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) stickyColor else Color(0x11FFFFFF),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        selectedNote = item
                                                        noteTitle = item.title
                                                        noteContent = item.content
                                                        noteCategory = item.category
                                                        noteColorHex = item.colorHex
                                                        isEditingMode = true
                                                    }
                                                    .padding(10.dp)
                                            ) {
                                                // Category Badge and Sticky Pin icon
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(stickyColor, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = item.category.uppercase(),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.PushPin,
                                                        contentDescription = null,
                                                        tint = stickyColor.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Text(
                                                    text = item.title.ifBlank { "Untitled Note" },
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = item.content,
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // RIGHT COLUMN: Editor workspace pane
                        if (!isCompact || isEditingMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(if (isCompact) 1f else 0.55f)
                                    .background(Color(0xFF131313))
                                    .padding(14.dp)
                            ) {
                                if (isEditingMode) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        if (isCompact) {
                                            // Navigation header on compact mobile screens to go back to notes explorer
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        isEditingMode = false
                                                        selectedNote = null
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (selectedNote == null) "New Sticky" else "Edit Sticky",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        // Title editing box
                                        OutlinedTextField(
                                    value = noteTitle,
                                    onValueChange = { noteTitle = it },
                                    placeholder = { Text("Note Title", fontSize = 12.sp, color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray,
                                        focusedBorderColor = UbuntuWarmOrange,
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Sticky Background Colors selector Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("STICKY COLOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    listOf("#E95420", "#4CAF50", "#2196F3", "#9C27B0", "#FFEB3B").forEach { hex ->
                                        val colorObj = Color(android.graphics.Color.parseColor(hex))
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(colorObj)
                                                .border(
                                                    width = 2.dp,
                                                    color = if (noteColorHex == hex) Color.White else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { noteColorHex = hex }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Category folder selector dropdown
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("LEDGER GROUP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    listOf("Personal", "Work", "Ideas", "System").forEach { grp ->
                                        val isSel = noteCategory == grp
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSel) UbuntuWarmOrange else Color(0xFF222222))
                                                .clickable { noteCategory = grp }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(grp, color = Color.White, fontSize = 9.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Body Rich context editor
                                OutlinedTextField(
                                    value = noteContent,
                                    onValueChange = { noteContent = it },
                                    placeholder = { Text("Write content logs here...", fontSize = 11.sp, color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray,
                                        focusedBorderColor = Color.DarkGray,
                                        unfocusedBorderColor = Color(0x22FFFFFF),
                                        focusedContainerColor = Color(0x33000000),
                                        unfocusedContainerColor = Color(0x33000000)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Save & Delete panel buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Save button
                                    Button(
                                        onClick = {
                                            if (noteTitle.isBlank() && noteContent.isBlank()) {
                                                Toast.makeText(context, "Note must contain a title or content body!", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }

                                            val currentID = selectedNote?.id ?: UUID.randomUUID().toString()
                                            val newItem = NotesAppItem(
                                                id = currentID,
                                                title = noteTitle,
                                                content = noteContent,
                                                dateModified = System.currentTimeMillis(),
                                                category = noteCategory,
                                                colorHex = noteColorHex
                                            )

                                            // Upsert note list
                                            notesList = notesList.filter { it.id != currentID } + newItem
                                            saveNotesList(context, notesList)
                                            isEditingMode = false
                                            Toast.makeText(context, "Note synced successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = UbuntuWarmOrange),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                    ) {
                                        Text("Save Document", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    // Delete button
                                    if (selectedNote != null) {
                                        Button(
                                            onClick = {
                                                notesList = notesList.filter { it.id != selectedNote!!.id }
                                                saveNotesList(context, notesList)
                                                selectedNote = null
                                                isEditingMode = false
                                                Toast.makeText(context, "Note purged from workspace!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            // Empty selection placeholder screen info
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No note document open", color = Color.Gray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Select an existing card from the left panel, or click 'New Sticky Note' to start draft ledger journals.", color = Color.DarkGray, fontSize = 9.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
}

// Preference files saving helper utils via basic robust JSON serialization format
private fun saveNotesList(context: Context, list: List<NotesAppItem>) {
    val array = JSONArray()
    list.forEach { note ->
        val obj = JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("content", note.content)
            put("dateModified", note.dateModified)
            put("category", note.category)
            put("colorHex", note.colorHex)
        }
        array.put(obj)
    }

    val sp = context.getSharedPreferences("isospace_preferences", Context.MODE_PRIVATE)
    sp.edit().putString("isospace_notes_vault", array.toString()).apply()
}

// Deserializer tool from shared preference block
private fun loadSavedNotes(context: Context): List<NotesAppItem> {
    val items = mutableListOf<NotesAppItem>()
    val sp = context.getSharedPreferences("isospace_preferences", Context.MODE_PRIVATE)
    val rawJson = sp.getString("isospace_notes_vault", null) ?: return emptyList()

    try {
        val array = JSONArray(rawJson)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            items.add(
                NotesAppItem(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    content = obj.getString("content"),
                    dateModified = obj.getLong("dateModified"),
                    category = obj.getString("category"),
                    colorHex = obj.getString("colorHex")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return items
}

@Composable
private fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    borderColor: Color = UbuntuWarmOrange,
    unfocusedBorderColor: Color = Color.DarkGray,
    containerColor: Color = Color(0x33000000),
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp
) {
    var isFocused by remember { mutableStateOf(false) }
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color.White,
            fontSize = fontSize,
            fontFamily = FontFamily.SansSerif
        ),
        singleLine = true,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(borderColor),
        modifier = modifier
            .background(containerColor, RoundedCornerShape(6.dp))
            .border(
                1.dp,
                if (isFocused) borderColor else unfocusedBorderColor,
                RoundedCornerShape(6.dp)
            )
            .onFocusChanged { isFocused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.Gray,
                            fontSize = fontSize
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear text",
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    )
}
