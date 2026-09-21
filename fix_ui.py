import os
import re

ui_dir = '/home/nicolas/Github/MoovAR/feature'
app_dir = '/home/nicolas/Github/MoovAR/app/src/main/java/com/moovar/android'

# 1. Replace collectAsState with collectAsStateWithLifecycle
for root, _, files in os.walk('/home/nicolas/Github/MoovAR'):
    for file in files:
        if file.endswith('.kt') and ('Screen' in file or 'App' in file):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
            
            if 'collectAsState(' in content:
                content = content.replace('collectAsState(', 'collectAsStateWithLifecycle(')
                if 'import androidx.lifecycle.compose.collectAsStateWithLifecycle' not in content:
                    content = content.replace('import androidx.compose.runtime.collectAsState\n', 'import androidx.compose.runtime.collectAsState\nimport androidx.lifecycle.compose.collectAsStateWithLifecycle\n')
                
                with open(path, 'w') as f:
                    f.write(content)

print("Applied collectAsStateWithLifecycle fixes")
