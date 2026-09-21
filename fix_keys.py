import os
import re

files_to_fix = [
    ('/home/nicolas/Github/MoovAR/feature/home/src/main/java/com/moovar/android/feature/home/HomeScreen.kt', 
     r'items\(uiState\.lines\)', r'items(uiState.lines, key = { it.id })'),
    ('/home/nicolas/Github/MoovAR/feature/alerts/src/main/java/com/moovar/android/feature/alerts/AlertsScreen.kt', 
     r'items\(uiState\.allAlerts\)', r'items(uiState.allAlerts, key = { it.id })'),
    ('/home/nicolas/Github/MoovAR/feature/favorites/src/main/java/com/moovar/android/feature/favorites/FavoritesScreen.kt', 
     r'items\(uiState\.favorites\)', r'items(uiState.favorites, key = { it.id })'),
    ('/home/nicolas/Github/MoovAR/feature/departures/src/main/java/com/moovar/android/feature/departures/StationSelectorScreen.kt', 
     r'items\(uiState\.branches\)', r'items(uiState.branches, key = { it.id })'),
    ('/home/nicolas/Github/MoovAR/feature/departures/src/main/java/com/moovar/android/feature/departures/StationSelectorScreen.kt', 
     r'items\(stations\)', r'items(stations, key = { it.id })'),
    ('/home/nicolas/Github/MoovAR/feature/journey/src/main/java/com/moovar/android/feature/journey/components/StopTimelineList.kt', 
     r'itemsIndexed\(stops, key = \{ index, stop -> "\$\{stop\.stationName\}_\$index" \}\)', r'itemsIndexed(stops, key = { index, stop -> "${stop.stationName}_${stop.scheduledTime}_$index" })')
]

for path, pattern, replacement in files_to_fix:
    if os.path.exists(path):
        with open(path, 'r') as f:
            content = f.read()
        new_content = re.sub(pattern, replacement, content)
        with open(path, 'w') as f:
            f.write(new_content)

print("Applied key fixes")
