package com.don.homefitness.feature.muscle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.don.homefitness.data.body.MuscleRegion

@Composable
fun MuscleTextList(
    regions: List<MuscleRegion>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, userScrollEnabled = true) {
        item {
            Text(
                text = "文字选择（模型不可用时仍可使用）",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(regions, key = { it.regionId }) { region ->
            val isSelected = selectedId == region.muscleGroupId
            ListItem(
                headlineContent = { Text(region.displayNameZh) },
                supportingContent = { Text(region.muscleGroupId) },
                leadingContent = {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onSelect(region.muscleGroupId) })
                    .semantics {
                        role = Role.RadioButton
                        selected = isSelected
                    },
            )
        }
    }
}
