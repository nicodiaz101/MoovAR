import re

# 1. StationEntity
with open('/home/nicolas/Github/MoovAR/core/database/src/main/java/com/moovar/android/core/database/entity/StationEntity.kt', 'r') as f:
    content = f.read()
content = content.replace('indices = [Index("branchId"), Index("name")]', 'indices = [Index("branchId"), Index("name"), Index("lineId")]')
with open('/home/nicolas/Github/MoovAR/core/database/src/main/java/com/moovar/android/core/database/entity/StationEntity.kt', 'w') as f:
    f.write(content)

# 2. RecentStationEntity
with open('/home/nicolas/Github/MoovAR/core/database/src/main/java/com/moovar/android/core/database/entity/RecentStationEntity.kt', 'r') as f:
    content = f.read()
if 'indices =' not in content:
    content = content.replace('@Entity(tableName = "recent_stations")', '@Entity(tableName = "recent_stations", indices = [Index("accessedAt")])')
    if 'import androidx.room.Index' not in content:
        content = content.replace('import androidx.room.Entity', 'import androidx.room.Entity\nimport androidx.room.Index')
with open('/home/nicolas/Github/MoovAR/core/database/src/main/java/com/moovar/android/core/database/entity/RecentStationEntity.kt', 'w') as f:
    f.write(content)

# 3. FavoriteRouteEntity
with open('/home/nicolas/Github/MoovAR/core/database/src/main/java/com/moovar/android/core/database/entity/FavoriteRouteEntity.kt', 'r') as f:
    content = f.read()
content = content.replace('indices = [Index(value = ["originStationId", "destinationStationId"], unique = true)]', 'indices = [Index(value = ["originStationId", "destinationStationId"], unique = true), Index("createdAt")]')
with open('/home/nicolas/Github/MoovAR/core/database/src/main/java/com/moovar/android/core/database/entity/FavoriteRouteEntity.kt', 'w') as f:
    f.write(content)

# 4. AlertEntity
with open('/home/nicolas/Github/MoovAR/core/database/src/main/java/com/moovar/android/core/database/entity/AlertEntity.kt', 'r') as f:
    content = f.read()
content = content.replace('indices = [Index("lineId")]', 'indices = [Index("lineId"), Index("cachedAt")]')
with open('/home/nicolas/Github/MoovAR/core/database/src/main/java/com/moovar/android/core/database/entity/AlertEntity.kt', 'w') as f:
    f.write(content)

print("Applied DB indexes")
