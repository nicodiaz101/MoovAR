import re

files_to_fix = [
    ('/home/nicolas/Github/MoovAR/app/src/main/java/com/moovar/android/data/repository/FavoriteRouteRepositoryImpl.kt', 
     r'\}\n$', r'}.flowOn(Dispatchers.IO)\n'),
    ('/home/nicolas/Github/MoovAR/app/src/main/java/com/moovar/android/data/repository/AlertRepositoryImpl.kt', 
     r'Result\.Success\(alerts\)\n\s*\}\n\s*\}$', r'Result.Success(alerts)\n        }.flowOn(Dispatchers.IO)\n    }')
]

for path, pattern, replacement in files_to_fix:
    with open(path, 'r') as f:
        content = f.read()
    
    # special handling to ensure flowOn is added properly without breaking syntax
    if 'flowOn(Dispatchers.IO)' not in content:
        if path.endswith('FavoriteRouteRepositoryImpl.kt'):
            content = content.replace('                )\n            }\n        }', '                )\n            }\n        }.flowOn(Dispatchers.IO)')
            if 'import kotlinx.coroutines.flow.flowOn' not in content:
                 content = content.replace('import kotlinx.coroutines.flow.map', 'import kotlinx.coroutines.flow.map\nimport kotlinx.coroutines.flow.flowOn')
        elif path.endswith('AlertRepositoryImpl.kt'):
            content = content.replace('            Result.Success(alerts)\n        }', '            Result.Success(alerts)\n        }.flowOn(Dispatchers.IO)')
            if 'import kotlinx.coroutines.flow.flowOn' not in content:
                 content = content.replace('import kotlinx.coroutines.flow.map', 'import kotlinx.coroutines.flow.map\nimport kotlinx.coroutines.flow.flowOn')

    with open(path, 'w') as f:
        f.write(content)

print("Applied flowOn fixes")
