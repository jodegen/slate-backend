# iOS Shortcuts – Health Import Guide

Base URL: `http://<SERVER-IP>:8080`  
Alle Endpoints: kein Auth-Token nötig (temporär)

---

## Endpoint 1: Steps

`POST /api/health/import/steps`

```json
{
  "steps":     "18\n54\n0\n...\n0",
  "dateTimes": "2026-05-14T00:56:15+02:00\n...\n2026-05-15T00:00:00+02:00"
}
```

### Shortcut-Konfiguration
| Feld | Wert |
|---|---|
| Type | Steps |
| Start Date | is today |
| Unit | count |
| Group By | Hour |
| Fill Missing | on |
| Sort By | Start Date, Oldest First |

### Felder
- `steps` → Values (step count per hour, newline-separated)
- `dateTimes` → Start Dates (timestamps, newline-separated)

### Serverseitige Logik
- **Datum** = Datum des ersten `dateTimes`-Eintrags
- **Gesamtschritte** = Summe aller `steps`-Werte
- Falls `totalSteps >= 10.000` → `"steps"` zu `RoutineLog.completedItems` hinzufügen

---

## Endpoint 2: Sleep

`POST /api/health/import/sleep`

```json
{
  "sleepStartTimes": "2026-04-19T02:34:38+02:00\n...",
  "sleepEndTimes":   "2026-04-19T02:48:08+02:00\n...\n2026-04-20T00:02:46+02:00",
  "sleepPhases":     "Core\nAwake\nCore\nDeep\n..."
}
```

### Shortcut-Konfiguration
| Feld | Wert |
|---|---|
| Type | Sleep Analysis |
| Start Date | yesterday |
| End Date | today |
| Sort By | Start Date, Oldest First |

### Felder
- `sleepStartTimes` → Start Dates
- `sleepEndTimes` → End Dates
- `sleepPhases` → Values (Core / REM / Deep / Awake / Asleep)

### Serverseitige Logik
- **Datum** = Datum des letzten `sleepEndTimes`-Eintrags (= Aufwachdatum, Mitternacht-Crossing korrekt)
- **durationMinutes** = Summe von `(endTime[N] - startTime[N])` wo `phase[N] != "Awake"`
- Upsert `SleepLog` für das Datum

### Phase-Werte (Apple Health)
| Value | Bedeutung | Zählt zur Schlafdauer |
|---|---|---|
| Core | Leichtschlaf | ✅ |
| Deep | Tiefschlaf | ✅ |
| REM | REM-Schlaf | ✅ |
| Asleep | Generisch schlafend | ✅ |
| Awake | Wach | ❌ |

---

## Endpoint 3: Workout

`POST /api/health/import/workout`

```json
{
  "workoutTypes":      "Running\nFunctional Strength Training",
  "workoutStartDates": "2026-05-14T07:00:00+02:00\n2026-05-14T18:00:00+02:00",
  "workoutEndDates":   "2026-05-14T07:45:00+02:00\n2026-05-14T19:00:00+02:00"
}
```

### Shortcut-Konfiguration
| Feld | Wert |
|---|---|
| Type | Workouts |
| Start Date | is today |

### Felder
- `workoutTypes` → Name (workout type string)
- `workoutStartDates` → Start Dates
- `workoutEndDates` → End Dates

### Serverseitige Logik
- **Datum** = Datum der `workoutStartDates[0]` (alle Workouts am selben Tag)
- **durationMinutes** = `(endDate - startDate)` pro Workout
- `TrainingSession` erstellen; `TrainingDay` erstellen falls nicht vorhanden

### Workout-Type-Mapping
| Apple Name | → TrainingType |
|---|---|
| Running | CARDIO |
| Cycling | CARDIO |
| Indoor Cycling | CARDIO |
| High Intensity Interval Training | CARDIO |
| Walking | CARDIO |
| Functional Strength Training | PUSH |
| Traditional Strength Training | PUSH |
| Core Training | PUSH |
| (alle anderen) | CARDIO |

---

## Was passiert serverseitig (Übersicht)

| Endpoint | Aktion |
|---|---|
| `/import/steps` | Upsert `RoutineLog`; falls ≥ 10.000 Steps → `"steps"` in `completedItems` |
| `/import/sleep` | Upsert `SleepLog` mit `durationMinutes` (nur Non-Awake-Stages) |
| `/import/workout` | `TrainingSession` + ggf. neuen `TrainingDay` erstellen |
