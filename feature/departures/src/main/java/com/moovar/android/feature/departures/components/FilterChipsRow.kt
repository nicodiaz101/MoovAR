package com.moovar.android.feature.departures.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moovar.android.core.domain.model.Branch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipsRow(
    branches: List<Branch>,
    selectedBranch: Branch?,
    onBranchSelected: (Branch?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier.padding(horizontal = 16.dp)) {
        items(branches) { branch ->
            FilterChip(
                selected = branch == selectedBranch,
                onClick = { 
                    if (branch == selectedBranch) onBranchSelected(null) 
                    else onBranchSelected(branch) 
                },
                label = { Text(branch.name) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}
