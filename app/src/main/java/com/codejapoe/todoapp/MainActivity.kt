package com.codejapoe.todoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codejapoe.todoapp.ui.theme.ToDoAppTheme
import android.content.Context
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.ContextCompat.startActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ToDoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ToDoApp()
                }
            }
        }
    }
}

data class ListItemData(
    val text: String,
    val isChecked: Boolean
)

object TaskManager {
    private val ongoingTasks = mutableStateListOf<ListItemData>()
    private val completedTasks = mutableStateListOf<ListItemData>()
    private const val fileName = "tasks.txt"

    fun addTask(context: Context, taskText: String) {
        if (taskText.isNotEmpty()) {
            val task = ListItemData(text = taskText, isChecked = false)
            ongoingTasks.add(task)
            saveTasksToFile(context)
        }
    }

    fun toggleTaskCompletion(context: Context, item: ListItemData, isChecked: Boolean) {
        val updatedTask = item.copy(isChecked = isChecked)
        if (isChecked) {
            ongoingTasks.remove(item)
            completedTasks.add(updatedTask)
        } else {
            completedTasks.remove(item)
            ongoingTasks.add(updatedTask)
        }
        saveTasksToFile(context)
    }

    fun removeTask(context: Context, item: ListItemData, fromOngoing: Boolean) {
        if (fromOngoing) {
            ongoingTasks.remove(item)
        } else {
            completedTasks.remove(item)
        }
        saveTasksToFile(context)
    }

    fun getOngoingTasks(): List<ListItemData> = ongoingTasks
    fun getCompletedTasks(): List<ListItemData> = completedTasks

    private fun saveTasksToFile(context: Context) {
        val file = File(context.filesDir, fileName)
        val tasksData = (ongoingTasks + completedTasks).joinToString("\n") { "${it.text}|${it.isChecked}" }

        try {
            FileOutputStream(file).use { output ->
                output.write(tasksData.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun loadTasksFromFile(context: Context) {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return

        try {
            file.forEachLine { line ->
                val parts = line.split("|")
                if (parts.size == 2) {
                    val text = parts[0]
                    val isChecked = parts[1].toBoolean()
                    val task = ListItemData(text, isChecked)

                    if (isChecked) {
                        completedTasks.add(task)
                    } else {
                        ongoingTasks.add(task)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

@Composable
fun NoRippleIconButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Task",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToDoApp() {
    var task by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        TaskManager.loadTasksFromFile(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        text = "To-do"
                    )
                },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info Icon"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(innerPadding)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = task,
                    onValueChange = { newValue ->
                        task = newValue.replace("|", "(or)")
                    },
                    label = { Text("Task") },
                    modifier = Modifier
                        .width(300.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                ) {
                    NoRippleIconButton(
                        onClick = {
                            TaskManager.addTask(context, task)
                            task = ""
                        }
                    )
                }
            }

            val tabs = listOf("Ongoing", "Completed")

            TabRow(
                selectedTabIndex = selectedTabIndex,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                val items = if (selectedTabIndex == 0) TaskManager.getOngoingTasks() else TaskManager.getCompletedTasks()
                items(items.size) { index ->
                    val item = items[index]
                    ListItem(
                        item = item,
                        onCheckedChange = { isChecked ->
                            TaskManager.toggleTaskCompletion(context, item, isChecked)
                        },
                        onIconClick = {
                            TaskManager.removeTask(context, item, selectedTabIndex == 0)
                        }
                    )
                }
            }
        }

        if (showDialog) {
            InfoDialog(context = context, onDismiss = { showDialog = false })
        }
    }
}

@Composable
fun ListItem(
    item: ListItemData,
    onCheckedChange: (Boolean) -> Unit,
    onIconClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(start = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.text,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )

        Checkbox(
            checked = item.isChecked,
            onCheckedChange = onCheckedChange
        )

        IconButton(onClick = onIconClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Icon"
            )
        }
    }
}

@Composable
fun InfoDialog(context: Context, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("About This App") },
        text = {
            Column {
                Text("This is a simple To-Do app that allows you to manage your ongoing and completed tasks.\n\nDeveloper: Codejapoe")

                Spacer(modifier = Modifier.height(8.dp))

                ClickableText(
                    text = buildAnnotatedString {
                        append("For more information, visit ")
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("codejapoe.xyz")
                        }
                    },
                    onClick = { offset ->
                        val linkStart = "For more information, visit ".length
                        if (offset in linkStart until linkStart + "codejapoe.xyz".length) {
                            openLinkInBrowser(context)
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("OK")
            }
        }
    )
}

private fun openLinkInBrowser(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://codejapoe.xyz"))
    startActivity(context, intent, null)
}

@Preview(showBackground = true)
@Composable
fun ToDoAppPreview() {
    ToDoAppTheme {
        ToDoApp()
    }
}