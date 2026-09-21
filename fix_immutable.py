import os
import re

directories = [
    '/home/nicolas/Github/MoovAR/core/domain/src/main/java/com/moovar/android/core/domain/model',
    '/home/nicolas/Github/MoovAR/feature'
]

for d in directories:
    for root, _, files in os.walk(d):
        for file in files:
            if file.endswith('.kt') and ('Model' in file or 'UiState' in file or file == 'Models.kt' or file == 'VehicleLocation.kt' or 'ViewModel' in file):
                path = os.path.join(root, file)
                with open(path, 'r') as f:
                    content = f.read()
                
                # For domain models
                if file in ['Models.kt', 'VehicleLocation.kt']:
                    content = content.replace('data class', '@androidx.compose.runtime.Immutable\ndata class')
                    if 'import androidx.compose.runtime.Immutable' not in content:
                        content = content.replace('package com.moovar.android.core.domain.model\n', 'package com.moovar.android.core.domain.model\n\nimport androidx.compose.runtime.Immutable\n')
                
                # For UiStates in ViewModels
                if 'ViewModel.kt' in file:
                    content = re.sub(r'(data class \w+UiState)', r'@androidx.compose.runtime.Immutable\n\1', content)
                    if '@androidx.compose.runtime.Immutable' in content and 'import androidx.compose.runtime.Immutable' not in content:
                         content = content.replace('package com.moovar.android.feature.', 'package com.moovar.android.feature.').replace('import androidx.lifecycle.ViewModel', 'import androidx.compose.runtime.Immutable\nimport androidx.lifecycle.ViewModel')

                with open(path, 'w') as f:
                    f.write(content)

print("Applied @Immutable fixes")
